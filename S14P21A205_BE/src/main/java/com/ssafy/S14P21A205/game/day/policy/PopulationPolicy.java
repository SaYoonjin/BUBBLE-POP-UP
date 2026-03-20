package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.time.model.DayWindow;
import com.ssafy.S14P21A205.game.time.policy.GameTimePolicy;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopulationPolicy {

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final int BUSINESS_OPEN_HOUR = GameTimePolicy.BUSINESS_OPEN_HOUR;
    private static final int BUSINESS_CLOSE_HOUR = GameTimePolicy.BUSINESS_CLOSE_HOUR;

    private final PopulationRepository populationRepository;
    private final TrafficRepository trafficRepository;
    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    public DaySchedule buildDaySchedule(Long locationId, int day, BigDecimal weatherMultiplier) {
        List<Population> populations = resolveDayRows(
                populationRepository.findByLocationIdOrderByDateAsc(locationId),
                Population::getDate,
                day
        );
        List<Traffic> traffics = resolveDayRows(
                trafficRepository.findByLocationIdOrderByDateAsc(locationId),
                Traffic::getDate,
                day
        );
        if (populations.isEmpty() || traffics.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        BigDecimal trafficBaseline = averageTrafficStatus(traffics);

        Map<Integer, Integer> populationByHour = new LinkedHashMap<>();
        for (Population population : populations) {
            int hour = population.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                populationByHour.put(hour, population.getFloatingPopulation());
            }
        }

        Map<Integer, Integer> trafficByHour = new LinkedHashMap<>();
        for (Traffic traffic : traffics) {
            int hour = traffic.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                trafficByHour.put(hour, traffic.getTrafficStatus().getValue());
            }
        }

        LinkedHashMap<String, GameDayStartResponse.HourlySchedule> hourlySchedule = new LinkedHashMap<>();
        List<BigDecimal> hourlyMultipliers = new ArrayList<>();
        BigDecimal resolvedWeatherMultiplier = normalizeRate(weatherMultiplier);
        for (int hour = BUSINESS_OPEN_HOUR; hour < BUSINESS_CLOSE_HOUR; hour++) {
            BigDecimal trafficMultiplier = trafficByHour.containsKey(hour)
                    ? ratio(trafficByHour.get(hour), trafficBaseline)
                    : DECIMAL_ONE;
            BigDecimal eventMultiplier = DECIMAL_ONE;
            int population = populationByHour.getOrDefault(hour, 0);
            int effectivePopulation = BigDecimal.valueOf(population)
                    .multiply(resolvedWeatherMultiplier)
                    .multiply(normalizeRate(trafficMultiplier))
                    .multiply(eventMultiplier)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            hourlySchedule.put(
                    String.valueOf(hour),
                    new GameDayStartResponse.HourlySchedule(
                            population,
                            trafficMultiplier,
                            eventMultiplier,
                            effectivePopulation
                    )
            );
            hourlyMultipliers.add(trafficMultiplier);
        }

        return new DaySchedule(hourlySchedule, average(hourlyMultipliers));
    }

    public int calculateCurrentPopulation(
            GameDayStartResponse startResponse,
            DayWindow currentTimeline,
            BigDecimal populationEventMultiplier,
            LocalDateTime effectiveNow
    ) {
        return resolvePopulationSnapshot(
                startResponse,
                currentTimeline,
                populationEventMultiplier,
                effectiveNow
        ).currentFloatingPopulation();
    }

    public PopulationSnapshot resolvePopulationSnapshot(
            GameDayStartResponse startResponse,
            DayWindow currentTimeline,
            BigDecimal populationEventMultiplier,
            LocalDateTime effectiveNow
    ) {
        if (!effectiveNow.isAfter(currentTimeline.businessStart()) || !effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return PopulationSnapshot.empty();
        }

        if (startResponse.hourlySchedule() == null || startResponse.hourlySchedule().isEmpty()) {
            return PopulationSnapshot.empty();
        }

        List<GameDayStartResponse.HourlySchedule> schedules = new ArrayList<>(startResponse.hourlySchedule().values());
        long totalMillis = seasonTimelineService.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int scheduleIndex = (int) Math.min(
                schedules.size() - 1L,
                (boundedElapsedMillis * schedules.size()) / totalMillis
        );
        GameDayStartResponse.HourlySchedule schedule = schedules.get(scheduleIndex);

        int baseFloatingPopulation = schedule.population() == null ? 0 : schedule.population();
        if (baseFloatingPopulation <= 0) {
            return PopulationSnapshot.empty();
        }

        BigDecimal populationGrowthRate = normalizeRate(startResponse.weatherMultiplier())
                .multiply(normalizeRate(schedule.trafficMultiplier()))
                .multiply(normalizeRate(schedule.eventMultiplier()))
                .multiply(normalizeRate(populationEventMultiplier))
                .setScale(4, RoundingMode.HALF_UP);

        int currentFloatingPopulation = BigDecimal.valueOf(baseFloatingPopulation)
                .multiply(populationGrowthRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        return new PopulationSnapshot(
                baseFloatingPopulation,
                populationGrowthRate,
                currentFloatingPopulation
        );
    }

    private BigDecimal averageTrafficStatus(List<Traffic> traffics) {
        BigDecimal total = BigDecimal.ZERO;
        for (Traffic traffic : traffics) {
            total = total.add(BigDecimal.valueOf(traffic.getTrafficStatus().getValue()));
        }
        return total.divide(BigDecimal.valueOf(traffics.size()), 6, RoundingMode.HALF_UP);
    }

    private <T> List<T> resolveDayRows(List<T> rows, Function<T, LocalDateTime> dateExtractor, int day) {
        if (rows.isEmpty() || day < 1) {
            return List.of();
        }

        Map<LocalDate, List<T>> rowsByDate = new LinkedHashMap<>();
        for (T row : rows) {
            rowsByDate.computeIfAbsent(dateExtractor.apply(row).toLocalDate(), key -> new ArrayList<>()).add(row);
        }

        List<List<T>> groupedRows = new ArrayList<>(rowsByDate.values());
        if (groupedRows.size() < day) {
            return List.of();
        }
        return groupedRows.get(day - 1);
    }

    private BigDecimal ratio(int trafficStatus, BigDecimal baseline) {
        if (baseline.compareTo(BigDecimal.ZERO) == 0) {
            return DECIMAL_ONE;
        }
        return BigDecimal.valueOf(trafficStatus).divide(baseline, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return DECIMAL_ONE;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value);
        }
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return DECIMAL_ONE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public PopulationLevel resolvePopulationLevel(Integer population) {
        return PopulationLevel.fromScore(population);
    }

    public String resolvePopulationLabel(Integer population) {
        return resolvePopulationLevel(population).label();
    }

    public record PopulationSnapshot(
            Integer baseFloatingPopulation,
            BigDecimal populationGrowthRate,
            Integer currentFloatingPopulation
    ) {
        public static PopulationSnapshot empty() {
            return new PopulationSnapshot(0, DECIMAL_ONE, 0);
        }
    }
}


