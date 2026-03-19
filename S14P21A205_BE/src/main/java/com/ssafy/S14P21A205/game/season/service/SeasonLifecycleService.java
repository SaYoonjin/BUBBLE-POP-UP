package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.scheduler.SeasonDayClosingScheduler;
import com.ssafy.S14P21A205.game.news.service.NewsService;
import com.ssafy.S14P21A205.game.news.service.SparkNewsDataService;
import com.ssafy.S14P21A205.game.environment.entity.Festival;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.Weather;
import com.ssafy.S14P21A205.game.environment.entity.WeatherLocation;
import com.ssafy.S14P21A205.game.environment.entity.WeatherType;
import com.ssafy.S14P21A205.game.environment.repository.FestivalRepository;
import com.ssafy.S14P21A205.game.environment.repository.PopulationRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherLocationRepository;
import com.ssafy.S14P21A205.game.environment.repository.WeatherRepository;
import com.ssafy.S14P21A205.game.event.entity.DailyEvent;
import com.ssafy.S14P21A205.game.event.entity.EventCategory;
import com.ssafy.S14P21A205.game.event.entity.EventEndTime;
import com.ssafy.S14P21A205.game.event.entity.EventStartTime;
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.event.repository.RandomEventRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.game.time.model.SeasonTimePoint;
import com.ssafy.S14P21A205.game.time.service.SeasonTimelineService;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SeasonLifecycleService {

    private static final BigDecimal DECIMAL_ONE = new BigDecimal("1.00");
    private static final BigDecimal ZERO_DECIMAL = new BigDecimal("0.00");
    private static final BigDecimal DISASTER_STOCK_HALF = new BigDecimal("0.50");
    private static final int EVENTS_PER_DAY = 2;
    private static final int FIRST_EVENT_OFFSET_SECONDS = 40;
    private static final int SECOND_EVENT_OFFSET_SECONDS = 80;
    private static final int FESTIVAL_DAY = 4;
    private static final int FESTIVAL_APPLY_OFFSET_SECONDS = 0;
    private static final int FESTIVAL_EXPIRE_OFFSET_SECONDS = 120;

    private final SeasonRepository seasonRepository;
    private final SeasonDayClosingScheduler seasonDayClosingScheduler;
    private final WeatherRepository weatherRepository;
    private final WeatherLocationRepository weatherLocationRepository;
    private final WeatherDayRedisRepository weatherDayRedisRepository;
    private final PopulationRepository populationRepository;
    private final TrafficRepository trafficRepository;
    private final TrafficDayRedisRepository trafficDayRedisRepository;
    private final DailyEventRepository dailyEventRepository;
    private final RandomEventRepository randomEventRepository;
    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;
    private final FestivalRepository festivalRepository;
    private final NewsService newsService;
    private final SparkNewsDataService sparkNewsDataService;

    private final SeasonTimelineService seasonTimelineService = new SeasonTimelineService();

    private Clock clock = Clock.systemDefaultZone();

    public void synchronize() {
        LocalDateTime now = LocalDateTime.now(clock);

        Season inProgressSeason = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS).orElse(null);
        if (inProgressSeason != null) {
            synchronizeInProgressSeason(inProgressSeason, now);
            return;
        }

        Season scheduledSeason = seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED).orElse(null);
        if (scheduledSeason == null || scheduledSeason.getStartTime() == null) {
            return;
        }

        // 시즌 준비 시간: startTime 전이면 이벤트 빌드 + ETL + 뉴스 미리 생성
        if (scheduledSeason.getStartTime().isAfter(now)) {
            prepareScheduledSeason(scheduledSeason);
            return;
        }

        List<Location> locations = requireLocations();
        String sourceBatchKey = resolveStartableSourceBatchKey(scheduledSeason, locations);
        if (sourceBatchKey == null) {
            return;
        }

        scheduledSeason.start(sourceBatchKey);
        List<Menu> menus = requireMenus();
        Random random = new Random(resolveSeed(scheduledSeason));

        List<WeatherLocation> weatherSchedule = rebuildWeatherSchedule(scheduledSeason, locations, random);
        List<Traffic> trafficSchedule = rebuildTrafficSchedule(scheduledSeason, locations, sourceBatchKey);
        rebuildPopulationSchedule(scheduledSeason, locations, sourceBatchKey);
        rebuildDailyEvents(scheduledSeason, menus, random);
        preloadWeatherDay(scheduledSeason.getId(), weatherSchedule, 1);
        preloadTrafficDay(scheduledSeason, trafficSchedule, 1);
        scheduledSeason.updateEndTime(resolveSeasonEndAt(scheduledSeason));

        // 시즌 준비 시간에 트렌드 뉴스가 이미 생성됐으면 축제 예고만 보충
        // 아직 뉴스가 없으면 전체 생성 (트렌드 + 축제 예고)
        try {
            newsService.generateSeasonNews(scheduledSeason.getId());
            newsService.generateEventPreviewNewsIfMissing(scheduledSeason.getId());
        } catch (Exception e) {
            log.error("Failed to generate season news. seasonId={}", scheduledSeason.getId(), e);
        }

        synchronizeInProgressSeason(scheduledSeason, now);
    }

    /**
     * 시즌 준비 시간 (SCHEDULED, startTime 전): ETL + 뉴스 미리 생성.
     * 이벤트는 아직 없으므로 축제 예고는 건너뛰고, IN_PROGRESS 전환 후 보충.
     */
    private void prepareScheduledSeason(Season scheduledSeason) {
        try {
            // ETL: 유동인구·교통량 미리 준비
            sparkNewsDataService.runNewsEtl();
            // 트렌드 뉴스 미리 생성 (이벤트 없으므로 축제 예고는 건너뜀)
            newsService.generateSeasonNews(scheduledSeason.getId());
        } catch (Exception e) {
            log.error("Failed to prepare scheduled season. seasonId={}", scheduledSeason.getId(), e);
        }
    }

    private void synchronizeInProgressSeason(Season season, LocalDateTime now) {
        LocalDateTime seasonEndAt = resolveSeasonEndAt(season);
        season.updateEndTime(seasonEndAt);
        seasonDayClosingScheduler.synchronize(season);

        SeasonTimePoint timePoint = seasonTimelineService.resolve(season, now);
        log.info(
                "season-timeline seasonId={} now={} phase={} day={} gameTime={} tick={} remaining={} joinEnabled={} playableFromDay={} seasonEndAt={} batchKey={}",
                season.getId(),
                now,
                timePoint.phase(),
                timePoint.currentDay(),
                timePoint.gameTime(),
                timePoint.tick(),
                timePoint.remainingPhaseSeconds(),
                timePoint.joinEnabled(),
                timePoint.joinPlayableFromDay(),
                seasonEndAt,
                season.getSourceBatchKey()
        );
        Integer targetDay = timePoint.currentDay();
        if (targetDay != null) {
            int previousDay = normalizeCurrentDay(season);
            if (targetDay > previousDay) {
                preloadReachedDays(season, previousDay + 1, targetDay);
            }
            season.syncCurrentDay(targetDay);
        }

        if (!now.isBefore(seasonEndAt)) {
            season.finish();
            seasonDayClosingScheduler.clear(season.getId());
            scheduleNextSeasonIfNeeded(season, seasonEndAt);
        }
    }

    private void preloadReachedDays(Season season, int startDay, int endDay) {
        if (season.getId() == null || season.getTotalDays() == null) {
            return;
        }

        int boundedStartDay = Math.max(1, startDay);
        int boundedEndDay = Math.min(endDay, season.getTotalDays());
        for (int day = boundedStartDay; day <= boundedEndDay; day++) {
            preloadWeatherDay(season.getId(), day);
            preloadTrafficDay(season, day);
        }
    }

    private List<WeatherLocation> rebuildWeatherSchedule(Season season, List<Location> locations, Random random) {
        Map<WeatherType, Weather> weatherByType = loadWeatherByType();
        weatherLocationRepository.deleteAllInBatch();

        List<WeatherLocation> weatherSchedule = new ArrayList<>(locations.size() * season.getTotalDays());
        for (Location location : locations) {
            for (int day = 1; day <= season.getTotalDays(); day++) {
                WeatherType weatherType = drawWeatherType(random);
                Weather weather = weatherByType.get(weatherType);
                if (weather == null) {
                    throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Weather master is missing: " + weatherType);
                }
                weatherSchedule.add(WeatherLocation.create(location, weather, day));
            }
        }

        return weatherLocationRepository.saveAll(weatherSchedule);
    }

    private void rebuildPopulationSchedule(Season season, List<Location> locations, String expectedSourceBatchKey) {
        List<Population> fixedRows = new ArrayList<>();
        List<LocalDate> expectedSourceDates = null;

        for (Location location : locations) {
            List<Population> sourceRows = populationRepository.findByLocationIdOrderByDateAsc(location.getId());
            ensureSourceBatchKey(location.getId(), "population", sourceRows, Population::getSourceBatchKey, expectedSourceBatchKey);
            List<List<Population>> dailyGroups = groupRowsByDate(sourceRows, Population::getDate);
            List<LocalDate> sourceDates = extractDistinctDates(sourceRows, Population::getDate);
            ensureExactSourceDays(location.getId(), "population", sourceDates.size(), season.getTotalDays());
            expectedSourceDates = ensureSameSourceDates(location.getId(), "population", expectedSourceDates, sourceDates);

            for (int day = 1; day <= season.getTotalDays(); day++) {
                for (Population population : dailyGroups.get(day - 1)) {
                    fixedRows.add(Population.create(
                            location,
                            normalizeSeasonDateTime(season, day, population.getDate()),
                            population.getFloatingPopulation(),
                            expectedSourceBatchKey
                    ));
                }
            }
        }

        populationRepository.deleteAllInBatch();
        populationRepository.saveAll(fixedRows);
    }

    private List<Traffic> rebuildTrafficSchedule(Season season, List<Location> locations, String expectedSourceBatchKey) {
        List<Traffic> fixedRows = new ArrayList<>();
        List<LocalDate> expectedSourceDates = null;

        for (Location location : locations) {
            List<Traffic> sourceRows = trafficRepository.findByLocationIdOrderByDateAsc(location.getId());
            ensureSourceBatchKey(location.getId(), "traffic", sourceRows, Traffic::getSourceBatchKey, expectedSourceBatchKey);
            List<List<Traffic>> dailyGroups = groupRowsByDate(sourceRows, Traffic::getDate);
            List<LocalDate> sourceDates = extractDistinctDates(sourceRows, Traffic::getDate);
            ensureExactSourceDays(location.getId(), "traffic", sourceDates.size(), season.getTotalDays());
            expectedSourceDates = ensureSameSourceDates(location.getId(), "traffic", expectedSourceDates, sourceDates);

            for (int day = 1; day <= season.getTotalDays(); day++) {
                for (Traffic traffic : dailyGroups.get(day - 1)) {
                    fixedRows.add(Traffic.create(
                            location,
                            normalizeSeasonDateTime(season, day, traffic.getDate()),
                            traffic.getTrafficStatus(),
                            expectedSourceBatchKey
                    ));
                }
            }
        }

        trafficRepository.deleteAllInBatch();
        return trafficRepository.saveAll(fixedRows);
    }

    private void rebuildDailyEvents(Season season, List<Menu> menus, Random random) {
        dailyEventRepository.deleteBySeasonId(season.getId());

        List<WeightedEventSpec> fullPool = buildWeightedEventPool(menus);
        List<DailyEvent> dailyEvents = new ArrayList<>(season.getTotalDays() * EVENTS_PER_DAY + 1);

        for (int day = 1; day <= season.getTotalDays(); day++) {
            List<WeightedEventSpec> eligiblePool = filterEligiblePool(fullPool, day, season.getTotalDays());
            for (int index = 0; index < EVENTS_PER_DAY; index++) {
                WeightedEventSpec selectedEvent = selectWeightedEvent(eligiblePool, random)
                        .withApplyOffsetSeconds(index == 0 ? FIRST_EVENT_OFFSET_SECONDS : SECOND_EVENT_OFFSET_SECONDS);
                RandomEvent randomEvent = upsertRandomEvent(selectedEvent);
                dailyEvents.add(DailyEvent.create(
                        season,
                        randomEvent,
                        day,
                        selectedEvent.applyOffsetSeconds(),
                        selectedEvent.expireOffsetSeconds(),
                        selectedEvent.targetLocationId(),
                        selectedEvent.targetMenuId()
                ));
            }
        }

        if (season.getTotalDays() >= FESTIVAL_DAY) {
            Festival festival = selectFestival(random);
            RandomEvent festivalEvent = upsertRandomEvent(new WeightedEventSpec(
                    EventCategory.FESTIVAL,
                    festival.getFestivalName(),
                    1.0,
                    EventStartTime.IMMEDIATE,
                    EventEndTime.SAME_DAY,
                    festival.getPopulationRate(),
                    ZERO_DECIMAL,
                    DECIMAL_ONE,
                    0,
                    FESTIVAL_APPLY_OFFSET_SECONDS,
                    FESTIVAL_EXPIRE_OFFSET_SECONDS,
                    festival.getLocation().getId(),
                    null
            ));
            dailyEvents.add(DailyEvent.create(
                    season,
                    festivalEvent,
                    FESTIVAL_DAY,
                    FESTIVAL_APPLY_OFFSET_SECONDS,
                    FESTIVAL_EXPIRE_OFFSET_SECONDS,
                    festival.getLocation().getId(),
                    null
            ));
        }

        dailyEventRepository.saveAll(dailyEvents);
    }

    private void preloadWeatherDay(Long seasonId, List<WeatherLocation> weatherSchedule, int day) {
        List<WeatherDayRedisRepository.WeatherDayEntry> dayEntries = weatherSchedule.stream()
                .filter(entry -> entry.getDay() == day)
                .map(entry -> new WeatherDayRedisRepository.WeatherDayEntry(
                        entry.getLocation().getId(),
                        entry.getDay(),
                        entry.getWeather().getWeatherType(),
                        entry.getWeather().getPopulationPercent()
                ))
                .toList();
        if (!dayEntries.isEmpty()) {
            weatherDayRedisRepository.saveDay(seasonId, day, dayEntries);
        }
    }

    private void preloadWeatherDay(Long seasonId, int day) {
        List<WeatherLocation> dayEntries = weatherLocationRepository.findByDayOrderByLocation_IdAsc(day);
        if (dayEntries.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Weather cache source is missing for day " + day);
        }

        weatherDayRedisRepository.saveDay(
                seasonId,
                day,
                dayEntries.stream()
                        .map(entry -> new WeatherDayRedisRepository.WeatherDayEntry(
                                entry.getLocation().getId(),
                                entry.getDay(),
                                entry.getWeather().getWeatherType(),
                                entry.getWeather().getPopulationPercent()
                        ))
                        .toList()
        );
    }

    private void preloadTrafficDay(Season season, List<Traffic> trafficSchedule, int day) {
        Map<Long, List<TrafficDayRedisRepository.TrafficEntry>> entriesByLocation = new LinkedHashMap<>();
        LocalDate targetDate = resolveSeasonStartDate(season).plusDays(day - 1L);
        for (Traffic traffic : trafficSchedule) {
            if (!targetDate.equals(traffic.getDate().toLocalDate())) {
                continue;
            }
            entriesByLocation.computeIfAbsent(traffic.getLocation().getId(), key -> new ArrayList<>())
                    .add(new TrafficDayRedisRepository.TrafficEntry(
                            traffic.getDate().getHour(),
                            traffic.getTrafficStatus()
                    ));
        }
        for (Map.Entry<Long, List<TrafficDayRedisRepository.TrafficEntry>> entry : entriesByLocation.entrySet()) {
            trafficDayRedisRepository.saveDay(season.getId(), entry.getKey(), day, entry.getValue());
        }
    }

    private void preloadTrafficDay(Season season, int day) {
        LocalDate targetDate = resolveSeasonStartDate(season).plusDays(day - 1L);
        for (Location location : requireLocations()) {
            List<Traffic> dayEntries = trafficRepository.findByLocation_IdAndDateBetweenOrderByDateAsc(
                    location.getId(),
                    targetDate.atStartOfDay(),
                    targetDate.plusDays(1).atStartOfDay().minusNanos(1)
            );
            if (dayEntries.isEmpty()) {
                throw new BaseException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Traffic cache source is missing for location " + location.getId() + " day " + day
                );
            }
            trafficDayRedisRepository.saveDay(
                    season.getId(),
                    location.getId(),
                    day,
                    dayEntries.stream()
                            .map(entry -> new TrafficDayRedisRepository.TrafficEntry(
                                    entry.getDate().getHour(),
                                    entry.getTrafficStatus()
                            ))
                            .toList()
                    );
        }
    }

    private String resolveStartableSourceBatchKey(Season scheduledSeason, List<Location> locations) {
        String populationBatchKey = resolveSharedSourceBatchKey(
                locations,
                "population",
                scheduledSeason.getTotalDays(),
                location -> populationRepository.findByLocationIdOrderByDateAsc(location.getId()),
                Population::getSourceBatchKey,
                Population::getDate
        );
        String trafficBatchKey = resolveSharedSourceBatchKey(
                locations,
                "traffic",
                scheduledSeason.getTotalDays(),
                location -> trafficRepository.findByLocationIdOrderByDateAsc(location.getId()),
                Traffic::getSourceBatchKey,
                Traffic::getDate
        );

        if (populationBatchKey == null || trafficBatchKey == null) {
            return null;
        }
        if (!populationBatchKey.equals(trafficBatchKey)) {
            log.info(
                    "Season {} is waiting for aligned spark batches. populationBatchKey={}, trafficBatchKey={}",
                    scheduledSeason.getId(),
                    populationBatchKey,
                    trafficBatchKey
            );
            return null;
        }

        String sourceBatchKey = populationBatchKey;
        boolean alreadyConsumed = seasonRepository.findFirstBySourceBatchKeyIsNotNullOrderByIdDesc()
                .map(Season::getSourceBatchKey)
                .filter(sourceBatchKey::equals)
                .isPresent();
        if (alreadyConsumed) {
            log.info(
                    "Season {} is waiting for a new spark batch. reusedBatchKey={}",
                    scheduledSeason.getId(),
                    sourceBatchKey
            );
            return null;
        }
        return sourceBatchKey;
    }

    private Map<WeatherType, Weather> loadWeatherByType() {
        List<Weather> weathers = weatherRepository.findAllByOrderByIdAsc();
        if (weathers.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Weather schedule source is missing.");
        }

        Map<WeatherType, Weather> weatherByType = new EnumMap<>(WeatherType.class);
        for (Weather weather : weathers) {
            weatherByType.put(weather.getWeatherType(), weather);
        }
        return weatherByType;
    }

    private List<Location> requireLocations() {
        List<Location> locations = locationRepository.findAllByOrderByIdAsc();
        if (locations.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Location source is missing.");
        }
        return locations;
    }

    private List<Menu> requireMenus() {
        List<Menu> menus = menuRepository.findAllByOrderByIdAsc();
        if (menus.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Menu source is missing.");
        }
        return menus;
    }

    private Festival selectFestival(Random random) {
        List<Festival> festivals = festivalRepository.findAllByOrderByIdAsc();
        if (festivals.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Festival source is missing.");
        }
        return festivals.get(random.nextInt(festivals.size()));
    }

    private <T> String resolveSharedSourceBatchKey(
            List<Location> locations,
            String sourceName,
            int requiredDays,
            Function<Location, List<T>> rowsLoader,
            Function<T, String> batchKeyExtractor,
            Function<T, LocalDateTime> dateExtractor
    ) {
        String expectedBatchKey = null;
        List<LocalDate> expectedDates = null;

        for (Location location : locations) {
            List<T> rows = rowsLoader.apply(location);
            String batchKey = extractSingleSourceBatchKey(rows, batchKeyExtractor);
            if (batchKey == null) {
                log.info(
                        "Season start is waiting for a single {} spark batch. locationId={}",
                        sourceName,
                        location.getId()
                );
                return null;
            }

            List<LocalDate> sourceDates = extractDistinctDates(rows, dateExtractor);
            if (sourceDates.size() != requiredDays) {
                log.info(
                        "Season start is waiting for {} {} days. locationId={} availableDays={}",
                        requiredDays,
                        sourceName,
                        location.getId(),
                        sourceDates.size()
                );
                return null;
            }
            if (expectedDates == null) {
                expectedDates = sourceDates;
            } else if (!expectedDates.equals(sourceDates)) {
                log.info(
                        "Season start is waiting for aligned {} source dates across locations. locationId={} expectedDates={} actualDates={}",
                        sourceName,
                        location.getId(),
                        expectedDates,
                        sourceDates
                );
                return null;
            }

            if (expectedBatchKey == null) {
                expectedBatchKey = batchKey;
                continue;
            }
            if (!expectedBatchKey.equals(batchKey)) {
                log.info(
                        "Season start is waiting for aligned {} spark batches across locations. expectedBatchKey={}, locationId={}, actualBatchKey={}",
                        sourceName,
                        expectedBatchKey,
                        location.getId(),
                        batchKey
                );
                return null;
            }
        }
        return expectedBatchKey;
    }

    private <T> void ensureSourceBatchKey(
            Long locationId,
            String sourceName,
            List<T> rows,
            Function<T, String> batchKeyExtractor,
            String expectedBatchKey
    ) {
        String actualBatchKey = extractSingleSourceBatchKey(rows, batchKeyExtractor);
        if (actualBatchKey == null) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    sourceName + " source batch is missing for location " + locationId
            );
        }
        if (!expectedBatchKey.equals(actualBatchKey)) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    sourceName + " source batch mismatch for location " + locationId
            );
        }
    }

    private <T> String extractSingleSourceBatchKey(List<T> rows, Function<T, String> batchKeyExtractor) {
        Set<String> batchKeys = new LinkedHashSet<>();
        for (T row : rows) {
            String batchKey = batchKeyExtractor.apply(row);
            if (batchKey == null || batchKey.isBlank()) {
                continue;
            }
            batchKeys.add(batchKey);
        }
        if (batchKeys.size() != 1) {
            return null;
        }
        return batchKeys.iterator().next();
    }

    private WeatherType drawWeatherType(Random random) {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return WeatherType.SUNNY;
        }
        if (roll < 65) {
            return WeatherType.RAIN;
        }
        if (roll < 80) {
            return WeatherType.SNOW;
        }
        if (roll < 85) {
            return WeatherType.HEATWAVE;
        }
        if (roll < 95) {
            return WeatherType.FOG;
        }
        return WeatherType.COLDWAVE;
    }

    private List<WeightedEventSpec> buildWeightedEventPool(List<Menu> menus) {
        List<WeightedEventSpec> pool = new ArrayList<>();
        pool.add(spec(
                EventCategory.CELEBRITY_APPEARANCE,
                "Celebrity Appearance",
                15.0,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("1.15"),
                ZERO_DECIMAL,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.SUBSTITUTE_HOLIDAY,
                "Substitute Holiday",
                10.0,
                EventStartTime.NEXT_DAY,
                EventEndTime.SAME_DAY,
                new BigDecimal("1.10"),
                ZERO_DECIMAL,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.GOVERNMENT_SUBSIDY,
                "Government Subsidy",
                10.0,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("1.05"),
                ZERO_DECIMAL,
                DECIMAL_ONE,
                200_000,
                null
        ));
        pool.add(spec(
                EventCategory.POLICY_CHANGE,
                "Policy Change",
                10.0,
                EventStartTime.NEXT_DAY,
                EventEndTime.SEASON_END,
                DECIMAL_ONE,
                ZERO_DECIMAL,
                new BigDecimal("1.05"),
                0,
                null
        ));
        pool.add(spec(
                EventCategory.INFECTIOUS_DISEASE,
                "Infectious Disease",
                10.0,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("0.70"),
                ZERO_DECIMAL,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.EARTHQUAKE,
                "Earthquake",
                3.75,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("0.80"),
                DISASTER_STOCK_HALF,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.FLOOD,
                "Flood",
                3.75,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("0.80"),
                DISASTER_STOCK_HALF,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.TYPHOON,
                "Typhoon",
                3.75,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("0.80"),
                DISASTER_STOCK_HALF,
                DECIMAL_ONE,
                0,
                null
        ));
        pool.add(spec(
                EventCategory.FIRE,
                "Fire",
                3.75,
                EventStartTime.IMMEDIATE,
                EventEndTime.SAME_DAY,
                new BigDecimal("0.80"),
                DISASTER_STOCK_HALF,
                DECIMAL_ONE,
                0,
                null
        ));

        for (Menu menu : menus) {
            pool.add(spec(
                    resolveMenuPriceCategory(menu.getMenuName(), true),
                    menu.getMenuName() + " price down",
                    1.5,
                    EventStartTime.IMMEDIATE,
                    EventEndTime.SEASON_END,
                    DECIMAL_ONE,
                    ZERO_DECIMAL,
                    new BigDecimal("0.95"),
                    0,
                    menu.getId()
            ));
            pool.add(spec(
                    resolveMenuPriceCategory(menu.getMenuName(), false),
                    menu.getMenuName() + " price up",
                    1.5,
                    EventStartTime.NEXT_DAY,
                    EventEndTime.SEASON_END,
                    DECIMAL_ONE,
                    ZERO_DECIMAL,
                    new BigDecimal("1.05"),
                    0,
                    menu.getId()
            ));
        }

        return pool;
    }

    private WeightedEventSpec spec(
            EventCategory category,
            String eventName,
            double weight,
            EventStartTime startTime,
            EventEndTime endTime,
            BigDecimal populationRate,
            BigDecimal stockFlat,
            BigDecimal costRate,
            Integer capitalFlat,
            Long targetMenuId
    ) {
        return new WeightedEventSpec(
                category,
                eventName,
                weight,
                startTime,
                endTime,
                populationRate,
                stockFlat,
                costRate,
                capitalFlat,
                0,
                null,
                null,
                targetMenuId
        );
    }

    private EventCategory resolveMenuPriceCategory(String menuName, boolean down) {
        String normalized = menuName == null ? "" : menuName.trim().toLowerCase().replace(" ", "");
        return switch (normalized) {
            case "빵", "bread" -> down ? EventCategory.BREAD_PRICE_DOWN : EventCategory.BREAD_PRICE_UP;
            case "말라스케워", "malaskewer", "mala_skewer" -> down ? EventCategory.MALA_SKEWER_PRICE_DOWN : EventCategory.MALA_SKEWER_PRICE_UP;
            case "젤리", "jelly" -> down ? EventCategory.JELLY_PRICE_DOWN : EventCategory.JELLY_PRICE_UP;
            case "떡볶이", "tteokbokki" -> down ? EventCategory.TTEOKBOKKI_PRICE_DOWN : EventCategory.TTEOKBOKKI_PRICE_UP;
            case "햄버거", "hamburger", "burger" -> down ? EventCategory.HAMBURGER_PRICE_DOWN : EventCategory.HAMBURGER_PRICE_UP;
            case "아이스크림", "icecream", "ice_cream" -> down ? EventCategory.ICE_CREAM_PRICE_DOWN : EventCategory.ICE_CREAM_PRICE_UP;
            case "닭강정", "dakgangjeong" -> down ? EventCategory.DAKGANGJEONG_PRICE_DOWN : EventCategory.DAKGANGJEONG_PRICE_UP;
            case "타코", "taco" -> down ? EventCategory.TACO_PRICE_DOWN : EventCategory.TACO_PRICE_UP;
            case "핫도그", "hotdog", "hot_dog" -> down ? EventCategory.HOTDOG_PRICE_DOWN : EventCategory.HOTDOG_PRICE_UP;
            case "버블티", "bubbletea", "bubble_tea" -> down ? EventCategory.BUBBLE_TEA_PRICE_DOWN : EventCategory.BUBBLE_TEA_PRICE_UP;
            default -> throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "Unsupported menu for event category: " + menuName);
        };
    }

    private List<WeightedEventSpec> filterEligiblePool(List<WeightedEventSpec> pool, int day, int totalDays) {
        if (day < totalDays) {
            return pool;
        }
        return pool.stream()
                .filter(event -> event.startTime() != EventStartTime.NEXT_DAY)
                .toList();
    }

    private WeightedEventSpec selectWeightedEvent(List<WeightedEventSpec> pool, Random random) {
        double totalWeight = 0.0;
        for (WeightedEventSpec event : pool) {
            totalWeight += event.weight();
        }

        double roll = random.nextDouble(totalWeight);
        double cumulativeWeight = 0.0;
        for (WeightedEventSpec event : pool) {
            cumulativeWeight += event.weight();
            if (roll < cumulativeWeight) {
                return event;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private RandomEvent upsertRandomEvent(WeightedEventSpec spec) {
        RandomEvent randomEvent = randomEventRepository
                .findFirstByEventCategoryAndEventName(spec.category(), spec.eventName())
                .orElseGet(() -> RandomEvent.create(
                        spec.category(),
                        spec.eventName(),
                        spec.startTime(),
                        spec.endTime(),
                        spec.populationRate(),
                        spec.stockFlat(),
                        spec.costRate(),
                        spec.capitalFlat()
                ));
        randomEvent.sync(
                spec.category(),
                spec.eventName(),
                spec.startTime(),
                spec.endTime(),
                spec.populationRate(),
                spec.stockFlat(),
                spec.costRate(),
                spec.capitalFlat()
        );
        return randomEventRepository.save(randomEvent);
    }

    private long resolveSeed(Season season) {
        long seasonId = season.getId() == null ? 0L : season.getId();
        long startEpochSecond = (season.getStartTime() == null ? LocalDateTime.now(clock) : season.getStartTime())
                .atZone(clock.getZone())
                .toEpochSecond();
        return seasonId * 31L + startEpochSecond;
    }

    private LocalDateTime normalizeSeasonDateTime(Season season, int day, LocalDateTime sourceDateTime) {
        LocalDate targetDate = resolveSeasonStartDate(season).plusDays(day - 1L);
        return LocalDateTime.of(targetDate, sourceDateTime.toLocalTime());
    }

    private LocalDate resolveSeasonStartDate(Season season) {
        return (season.getStartTime() == null ? LocalDateTime.now(clock) : season.getStartTime()).toLocalDate();
    }

    private int normalizeCurrentDay(Season season) {
        if (season.getTotalDays() == null || season.getTotalDays() < 1) {
            return 1;
        }
        int currentDay = season.getCurrentDay() == null ? 1 : season.getCurrentDay();
        if (currentDay < 1) {
            return 1;
        }
        return Math.min(currentDay, season.getTotalDays());
    }

    private <T> List<LocalDate> extractDistinctDates(List<T> rows, Function<T, LocalDateTime> dateExtractor) {
        Map<LocalDate, Boolean> dates = new LinkedHashMap<>();
        for (T row : rows) {
            dates.putIfAbsent(dateExtractor.apply(row).toLocalDate(), Boolean.TRUE);
        }
        return new ArrayList<>(dates.keySet());
    }

    private <T> List<List<T>> groupRowsByDate(List<T> rows, Function<T, LocalDateTime> dateExtractor) {
        Map<LocalDate, List<T>> rowsByDate = new LinkedHashMap<>();
        for (T row : rows) {
            rowsByDate.computeIfAbsent(dateExtractor.apply(row).toLocalDate(), key -> new ArrayList<>()).add(row);
        }
        return new ArrayList<>(rowsByDate.values());
    }

    private void ensureExactSourceDays(Long locationId, String sourceName, int availableDays, int requiredDays) {
        if (availableDays != requiredDays) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Expected exactly " + requiredDays + " " + sourceName + " source days for location " + locationId
            );
        }
    }

    private List<LocalDate> ensureSameSourceDates(
            Long locationId,
            String sourceName,
            List<LocalDate> expectedDates,
            List<LocalDate> actualDates
    ) {
        if (expectedDates == null) {
            return actualDates;
        }
        if (!expectedDates.equals(actualDates)) {
            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Mismatched " + sourceName + " source dates for location " + locationId
            );
        }
        return expectedDates;
    }

    private LocalDateTime resolveSeasonEndAt(Season season) {
        return seasonTimelineService.resolveNextSeasonStartAt(season);
    }

    private void scheduleNextSeasonIfNeeded(Season finishedSeason, LocalDateTime nextSeasonStartAt) {
        if (seasonRepository.existsByStatusAndStartTime(SeasonStatus.SCHEDULED, nextSeasonStartAt)) {
            return;
        }

        int totalDays = finishedSeason.getTotalDays() == null || finishedSeason.getTotalDays() <= 0
                ? 7
                : finishedSeason.getTotalDays();
        LocalDateTime nextSeasonEndAt = nextSeasonStartAt.plus(seasonTimelineService.seasonCycleDuration(totalDays));
        seasonRepository.save(Season.createScheduled(totalDays, nextSeasonStartAt, nextSeasonEndAt));
    }

    private record WeightedEventSpec(
            EventCategory category,
            String eventName,
            double weight,
            EventStartTime startTime,
            EventEndTime endTime,
            BigDecimal populationRate,
            BigDecimal stockFlat,
            BigDecimal costRate,
            Integer capitalFlat,
            Integer applyOffsetSeconds,
            Integer expireOffsetSeconds,
            Long targetLocationId,
            Long targetMenuId
    ) {
        private WeightedEventSpec withApplyOffsetSeconds(Integer nextApplyOffsetSeconds) {
            return new WeightedEventSpec(
                    category,
                    eventName,
                    weight,
                    startTime,
                    endTime,
                    populationRate,
                    stockFlat,
                    costRate,
                    capitalFlat,
                    nextApplyOffsetSeconds,
                    expireOffsetSeconds,
                    targetLocationId,
                    targetMenuId
            );
        }
    }
}
