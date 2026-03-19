package com.ssafy.S14P21A205.game.season.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.day.scheduler.SeasonDayClosingScheduler;
import com.ssafy.S14P21A205.game.environment.entity.Festival;
import com.ssafy.S14P21A205.game.environment.entity.Population;
import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
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
import com.ssafy.S14P21A205.game.event.entity.RandomEvent;
import com.ssafy.S14P21A205.game.event.repository.DailyEventRepository;
import com.ssafy.S14P21A205.game.event.repository.RandomEventRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeasonLifecycleServiceTests {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private SeasonDayClosingScheduler seasonDayClosingScheduler;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private WeatherLocationRepository weatherLocationRepository;

    @Mock
    private WeatherDayRedisRepository weatherDayRedisRepository;

    @Mock
    private PopulationRepository populationRepository;

    @Mock
    private TrafficRepository trafficRepository;

    @Mock
    private TrafficDayRedisRepository trafficDayRedisRepository;

    @Mock
    private DailyEventRepository dailyEventRepository;

    @Mock
    private RandomEventRepository randomEventRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private FestivalRepository festivalRepository;

    private SeasonLifecycleService seasonLifecycleService;

    @BeforeEach
    void setUp() {
        seasonLifecycleService = new SeasonLifecycleService(
                seasonRepository,
                seasonDayClosingScheduler,
                weatherRepository,
                weatherLocationRepository,
                weatherDayRedisRepository,
                populationRepository,
                trafficRepository,
                trafficDayRedisRepository,
                dailyEventRepository,
                randomEventRepository,
                locationRepository,
                menuRepository,
                festivalRepository
        );
        org.mockito.Mockito.lenient()
                .when(weatherLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(trafficRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient()
                .when(randomEventRepository.findFirstByEventCategoryAndEventName(any(), any()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(randomEventRepository.save(any(RandomEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void synchronizeStartsScheduledSeasonUsingFreshSourceBatch() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        LocalDateTime now = seasonStartAt.plusSeconds(1);
        Season scheduledSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        ReflectionTestUtils.setField(scheduledSeason, "id", 11L);

        Location location = location(3L);
        Menu menu = menu(5L, "bread");
        String batchKey = "spark-20260318-01";

        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(now.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"))
        );

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(seasonRepository.findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus.SCHEDULED))
                .thenReturn(Optional.of(scheduledSeason));
        when(seasonRepository.findFirstBySourceBatchKeyIsNotNullOrderByIdDesc()).thenReturn(Optional.empty());
        when(locationRepository.findAllByOrderByIdAsc()).thenReturn(List.of(location));
        when(menuRepository.findAllByOrderByIdAsc()).thenReturn(List.of(menu));
        when(weatherRepository.findAllByOrderByIdAsc()).thenReturn(allWeatherMasters());
        when(festivalRepository.findAllByOrderByIdAsc()).thenReturn(List.of(festival(location)));
        when(populationRepository.findByLocationIdOrderByDateAsc(3L))
                .thenReturn(populations(location, batchKey, seasonStartAt.toLocalDate()));
        when(trafficRepository.findByLocationIdOrderByDateAsc(3L))
                .thenReturn(traffics(location, batchKey, seasonStartAt.toLocalDate()));

        seasonLifecycleService.synchronize();

        assertThat(scheduledSeason.getStatus()).isEqualTo(SeasonStatus.IN_PROGRESS);
        assertThat(scheduledSeason.getCurrentDay()).isEqualTo(1);
        assertThat(scheduledSeason.getSourceBatchKey()).isEqualTo(batchKey);
        verify(weatherDayRedisRepository).saveDay(eq(11L), eq(1), any());
        verify(trafficDayRedisRepository).saveDay(eq(11L), eq(3L), eq(1), any());
        verify(seasonDayClosingScheduler).synchronize(scheduledSeason);
    }

    @Test
    void synchronizePreloadsReachedDayWhenTimelineAdvances() {
        LocalDateTime seasonStartAt = LocalDateTime.of(2026, 3, 18, 10, 0, 0);
        LocalDateTime now = seasonStartAt.plusSeconds(120 + 180 + 5L);
        Season inProgressSeason = Season.createScheduled(7, seasonStartAt, seasonStartAt.plusMinutes(30));
        ReflectionTestUtils.setField(inProgressSeason, "id", 21L);
        inProgressSeason.start("spark-20260318-02");

        Location location = location(3L);
        Weather weather = weather(WeatherType.SUNNY, 1L);
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(inProgressSeason));
        when(locationRepository.findAllByOrderByIdAsc()).thenReturn(List.of(location));
        when(weatherLocationRepository.findByDayOrderByLocation_IdAsc(2)).thenReturn(List.of(weatherLocation(location, weather, 2)));
        when(trafficRepository.findByLocation_IdAndDateBetweenOrderByDateAsc(eq(3L), any(), any()))
                .thenReturn(List.of(traffic(location, seasonStartAt.toLocalDate().plusDays(1), 10, "spark-20260318-02")));

        ReflectionTestUtils.setField(
                seasonLifecycleService,
                "clock",
                Clock.fixed(Instant.from(now.atZone(ZoneId.of("Asia/Seoul"))), ZoneId.of("Asia/Seoul"))
        );

        seasonLifecycleService.synchronize();

        assertThat(inProgressSeason.getCurrentDay()).isEqualTo(2);
        verify(weatherDayRedisRepository).saveDay(eq(21L), eq(2), any());
        verify(trafficDayRedisRepository).saveDay(eq(21L), eq(3L), eq(2), any());
        verify(seasonDayClosingScheduler).synchronize(inProgressSeason);
    }

    private List<Weather> allWeatherMasters() {
        return List.of(
                weather(WeatherType.SUNNY, 1L),
                weather(WeatherType.RAIN, 2L),
                weather(WeatherType.SNOW, 3L),
                weather(WeatherType.HEATWAVE, 4L),
                weather(WeatherType.FOG, 5L),
                weather(WeatherType.COLDWAVE, 6L)
        );
    }

    private List<Population> populations(Location location, String batchKey, java.time.LocalDate startDate) {
        return List.of(
                population(location, startDate.plusDays(0), 10, batchKey),
                population(location, startDate.plusDays(1), 10, batchKey),
                population(location, startDate.plusDays(2), 10, batchKey),
                population(location, startDate.plusDays(3), 10, batchKey),
                population(location, startDate.plusDays(4), 10, batchKey),
                population(location, startDate.plusDays(5), 10, batchKey),
                population(location, startDate.plusDays(6), 10, batchKey)
        );
    }

    private List<Traffic> traffics(Location location, String batchKey, java.time.LocalDate startDate) {
        return List.of(
                traffic(location, startDate.plusDays(0), 10, batchKey),
                traffic(location, startDate.plusDays(1), 10, batchKey),
                traffic(location, startDate.plusDays(2), 10, batchKey),
                traffic(location, startDate.plusDays(3), 10, batchKey),
                traffic(location, startDate.plusDays(4), 10, batchKey),
                traffic(location, startDate.plusDays(5), 10, batchKey),
                traffic(location, startDate.plusDays(6), 10, batchKey)
        );
    }

    private Location location(Long id) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "locationName", "loc-" + id);
        ReflectionTestUtils.setField(location, "rent", 100_000);
        ReflectionTestUtils.setField(location, "interiorCost", 200_000);
        return location;
    }

    private Menu menu(Long id, String menuName) {
        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", id);
        ReflectionTestUtils.setField(menu, "menuName", menuName);
        ReflectionTestUtils.setField(menu, "originPrice", 1_200);
        return menu;
    }

    private Weather weather(WeatherType type, Long id) {
        Weather weather = instantiate(Weather.class);
        ReflectionTestUtils.setField(weather, "id", id);
        ReflectionTestUtils.setField(weather, "weatherType", type);
        ReflectionTestUtils.setField(weather, "populationPercent", BigDecimal.ONE);
        return weather;
    }

    private WeatherLocation weatherLocation(Location location, Weather weather, int day) {
        WeatherLocation weatherLocation = instantiate(WeatherLocation.class);
        ReflectionTestUtils.setField(weatherLocation, "location", location);
        ReflectionTestUtils.setField(weatherLocation, "weather", weather);
        ReflectionTestUtils.setField(weatherLocation, "day", day);
        return weatherLocation;
    }

    private Festival festival(Location location) {
        Festival festival = instantiate(Festival.class);
        ReflectionTestUtils.setField(festival, "id", 31L);
        ReflectionTestUtils.setField(festival, "location", location);
        ReflectionTestUtils.setField(festival, "festivalName", "Fixture Festival");
        ReflectionTestUtils.setField(festival, "populationRate", new BigDecimal("1.20"));
        return festival;
    }

    private Population population(Location location, java.time.LocalDate date, int hour, String batchKey) {
        Population population = instantiate(Population.class);
        ReflectionTestUtils.setField(population, "location", location);
        ReflectionTestUtils.setField(population, "date", LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)));
        ReflectionTestUtils.setField(population, "floatingPopulation", 120);
        ReflectionTestUtils.setField(population, "sourceBatchKey", batchKey);
        return population;
    }

    private Traffic traffic(Location location, java.time.LocalDate date, int hour, String batchKey) {
        Traffic traffic = instantiate(Traffic.class);
        ReflectionTestUtils.setField(traffic, "location", location);
        ReflectionTestUtils.setField(traffic, "date", LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)));
        ReflectionTestUtils.setField(traffic, "trafficStatus", TrafficStatus.NORMAL);
        ReflectionTestUtils.setField(traffic, "sourceBatchKey", batchKey);
        return traffic;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
