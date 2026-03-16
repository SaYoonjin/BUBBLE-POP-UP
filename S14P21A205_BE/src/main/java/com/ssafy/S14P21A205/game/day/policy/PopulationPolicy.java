package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.model.DaySchedule;
import com.ssafy.S14P21A205.game.day.service.SeasonTimeline;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
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
    private static final int BUSINESS_OPEN_HOUR = SeasonTimeline.BUSINESS_OPEN_HOUR;
    private static final int BUSINESS_CLOSE_HOUR = SeasonTimeline.BUSINESS_CLOSE_HOUR;

    private final PopulationRepository populationRepository;
    private final TrafficRepository trafficRepository;
    private final SeasonTimeline seasonTimeline = new SeasonTimeline();

    public DaySchedule buildDaySchedule(Long locationId, int day) {
        List<Population> populations = populationRepository.findByLocationIdOrderByDateAsc(locationId);
        List<Traffic> traffics = trafficRepository.findByLocationIdOrderByDateAsc(locationId);
        if (populations.isEmpty() || traffics.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        List<Population> selectedPopulations = selectRowsForDay(populations, Population::getDate, day);
        List<Traffic> selectedTraffics = selectRowsForDay(traffics, Traffic::getDate, day);
        BigDecimal trafficBaseline = averageTrafficStatus(traffics);

        Map<Integer, Integer> populationByHour = new LinkedHashMap<>();
        for (Population population : selectedPopulations) {
            int hour = population.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                populationByHour.put(hour, population.getFloatingPopulation());
            }
        }

        Map<Integer, Integer> trafficByHour = new LinkedHashMap<>();
        for (Traffic traffic : selectedTraffics) {
            int hour = traffic.getDate().getHour();
            if (hour >= BUSINESS_OPEN_HOUR && hour < BUSINESS_CLOSE_HOUR) {
                trafficByHour.put(hour, traffic.getTrafficStatus().getValue());
            }
        }

        LinkedHashMap<String, GameDayStartResponse.HourlySchedule> hourlySchedule = new LinkedHashMap<>();
        List<BigDecimal> hourlyMultipliers = new ArrayList<>();
        for (int hour = BUSINESS_OPEN_HOUR; hour < BUSINESS_CLOSE_HOUR; hour++) {
            BigDecimal trafficMultiplier = trafficByHour.containsKey(hour)
                    ? ratio(trafficByHour.get(hour), trafficBaseline)
                    : DECIMAL_ONE;
            hourlySchedule.put(
                    String.valueOf(hour),
                    new GameDayStartResponse.HourlySchedule(
                            populationByHour.getOrDefault(hour, 0),
                            trafficMultiplier
                    )
            );
            hourlyMultipliers.add(trafficMultiplier);
        }

        return new DaySchedule(hourlySchedule, average(hourlyMultipliers));
    }

    public int calculateCurrentPopulation(
            GameDayStartResponse startResponse,
            SeasonTimeline.DayTimeline currentTimeline,
            BigDecimal populationEventMultiplier,
            LocalDateTime effectiveNow
    ) {
        if (!effectiveNow.isAfter(currentTimeline.businessStart()) || !effectiveNow.isBefore(currentTimeline.businessEnd())) {
            return 0;
        }

        if (startResponse.hourlySchedule() == null || startResponse.hourlySchedule().isEmpty()) {
            return 0;
        }

        List<GameDayStartResponse.HourlySchedule> schedules = new ArrayList<>(startResponse.hourlySchedule().values());
        long totalMillis = seasonTimeline.businessDuration().toMillis();
        long elapsedMillis = Duration.between(currentTimeline.businessStart(), effectiveNow).toMillis();
        long boundedElapsedMillis = Math.max(0L, Math.min(elapsedMillis, totalMillis));
        int scheduleIndex = (int) Math.min(
                schedules.size() - 1L,
                (boundedElapsedMillis * schedules.size()) / totalMillis
        );
        GameDayStartResponse.HourlySchedule schedule = schedules.get(scheduleIndex);

        BigDecimal population = BigDecimal.valueOf(schedule.population())
                .multiply(normalizeRate(startResponse.weatherMultiplier()))
                .multiply(normalizeRate(schedule.trafficMultiplier()))
                .multiply(normalizeRate(populationEventMultiplier));
        return population.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private <T> List<T> selectRowsForDay(List<T> rows, Function<T, LocalDateTime> dateExtractor, int day) {
        Map<LocalDate, List<T>> rowsByDate = new LinkedHashMap<>();
        for (T row : rows) {
            rowsByDate.computeIfAbsent(dateExtractor.apply(row).toLocalDate(), key -> new ArrayList<>()).add(row);
        }

        List<List<T>> groupedRows = new ArrayList<>(rowsByDate.values());
        if (groupedRows.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        int index = Math.floorMod(day - 1, groupedRows.size());
        return groupedRows.get(index);
    }

    private BigDecimal averageTrafficStatus(List<Traffic> traffics) {
        BigDecimal total = BigDecimal.ZERO;
        for (Traffic traffic : traffics) {
            total = total.add(BigDecimal.valueOf(traffic.getTrafficStatus().getValue()));
        }
        return total.divide(BigDecimal.valueOf(traffics.size()), 6, RoundingMode.HALF_UP);
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
}
