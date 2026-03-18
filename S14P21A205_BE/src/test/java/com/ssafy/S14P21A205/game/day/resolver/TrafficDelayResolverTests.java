package com.ssafy.S14P21A205.game.day.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import com.ssafy.S14P21A205.game.environment.entity.TrafficStatus;
import com.ssafy.S14P21A205.game.environment.repository.TrafficDayRedisRepository;
import com.ssafy.S14P21A205.game.environment.repository.TrafficRepository;
import com.ssafy.S14P21A205.store.entity.Location;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TrafficDelayResolverTests {

    @Mock
    private TrafficDayRedisRepository trafficDayRedisRepository;

    @Mock
    private TrafficRepository trafficRepository;

    private TrafficDelayResolver trafficDelayResolver;

    @BeforeEach
    void setUp() {
        trafficDelayResolver = new TrafficDelayResolver(trafficDayRedisRepository, trafficRepository);
    }

    @Test
    void resolveReturnsRedisTrafficForExactDayAndHour() {
        Long locationId = 3L;
        LocalDateTime currentDayStart = LocalDateTime.of(2026, 3, 17, 10, 0);
        LocalDateTime effectiveNow = LocalDateTime.of(2026, 3, 17, 10, 1, 15);
        LocalDateTime targetDateTime = LocalDateTime.of(2023, 12, 9, 12, 0);

        when(trafficRepository.findByLocationIdOrderByDateAsc(locationId)).thenReturn(List.of(
                traffic(locationId, LocalDateTime.of(2023, 12, 8, 10, 0), TrafficStatus.SMOOTH),
                traffic(locationId, targetDateTime, TrafficStatus.NORMAL)
        ));
        when(trafficDayRedisRepository.findExact(locationId, targetDateTime))
                .thenReturn(Optional.of(new TrafficDayRedisRepository.TrafficEntry(
                        targetDateTime,
                        TrafficStatus.CONGESTED
                )));

        TrafficDelayResolver.ResolvedTraffic resolvedTraffic = trafficDelayResolver.resolve(
                locationId,
                2,
                7,
                currentDayStart,
                effectiveNow
        );

        assertThat(resolvedTraffic.resolvedDateTime()).isEqualTo(targetDateTime);
        assertThat(resolvedTraffic.trafficStatus()).isEqualTo(TrafficStatus.CONGESTED);
        assertThat(resolvedTraffic.delaySeconds()).isEqualTo(25);
    }

    @Test
    void resolveFallsBackToDatabaseExactDatetimeWhenRedisMisses() {
        Long locationId = 3L;
        LocalDateTime currentDayStart = LocalDateTime.of(2026, 3, 17, 10, 0);
        LocalDateTime effectiveNow = LocalDateTime.of(2026, 3, 17, 10, 1, 0);
        LocalDateTime targetDateTime = LocalDateTime.of(2023, 12, 9, 11, 0);
        Traffic traffic = traffic(locationId, targetDateTime, TrafficStatus.VERY_SMOOTH);

        when(trafficRepository.findByLocationIdOrderByDateAsc(locationId)).thenReturn(List.of(
                traffic(locationId, LocalDateTime.of(2023, 12, 8, 10, 0), TrafficStatus.SMOOTH),
                traffic(locationId, targetDateTime, TrafficStatus.VERY_SMOOTH)
        ));
        when(trafficDayRedisRepository.findExact(locationId, targetDateTime)).thenReturn(Optional.empty());
        when(trafficRepository.findFirstByLocation_IdAndDate(locationId, targetDateTime)).thenReturn(Optional.of(traffic));

        TrafficDelayResolver.ResolvedTraffic resolvedTraffic = trafficDelayResolver.resolve(
                locationId,
                2,
                7,
                currentDayStart,
                effectiveNow
        );

        assertThat(resolvedTraffic.resolvedDateTime()).isEqualTo(targetDateTime);
        assertThat(resolvedTraffic.trafficStatus()).isEqualTo(TrafficStatus.VERY_SMOOTH);
        assertThat(resolvedTraffic.delaySeconds()).isEqualTo(5);
    }

    @Test
    void resolveReturnsNormalFallbackWhenExactTrafficIsMissing() {
        Long locationId = 3L;
        LocalDateTime currentDayStart = LocalDateTime.of(2026, 3, 17, 10, 0);
        LocalDateTime effectiveNow = LocalDateTime.of(2026, 3, 17, 10, 1, 35);
        LocalDateTime targetDateTime = LocalDateTime.of(2023, 12, 9, 14, 0);

        when(trafficRepository.findByLocationIdOrderByDateAsc(locationId)).thenReturn(List.of(
                traffic(locationId, LocalDateTime.of(2023, 12, 8, 10, 0), TrafficStatus.SMOOTH),
                traffic(locationId, LocalDateTime.of(2023, 12, 9, 10, 0), TrafficStatus.SMOOTH)
        ));
        when(trafficDayRedisRepository.findExact(locationId, targetDateTime)).thenReturn(Optional.empty());
        when(trafficRepository.findFirstByLocation_IdAndDate(locationId, targetDateTime)).thenReturn(Optional.empty());

        TrafficDelayResolver.ResolvedTraffic resolvedTraffic = trafficDelayResolver.resolve(
                locationId,
                2,
                7,
                currentDayStart,
                effectiveNow
        );

        assertThat(resolvedTraffic.resolvedDateTime()).isEqualTo(targetDateTime);
        assertThat(resolvedTraffic.trafficStatus()).isEqualTo(TrafficStatus.NORMAL);
        assertThat(resolvedTraffic.delaySeconds()).isEqualTo(20);
    }

    @Test
    void resolveReturnsNormalFallbackWhenTrafficDatesDoNotExist() {
        Long locationId = 3L;

        when(trafficRepository.findByLocationIdOrderByDateAsc(locationId)).thenReturn(List.of());

        TrafficDelayResolver.ResolvedTraffic resolvedTraffic = trafficDelayResolver.resolve(
                locationId,
                1,
                7,
                LocalDateTime.of(2026, 3, 17, 10, 0),
                LocalDateTime.of(2026, 3, 17, 10, 1, 0)
        );

        assertThat(resolvedTraffic.resolvedDateTime()).isNull();
        assertThat(resolvedTraffic.trafficStatus()).isEqualTo(TrafficStatus.NORMAL);
        assertThat(resolvedTraffic.delaySeconds()).isEqualTo(20);
    }

    private Traffic traffic(Long locationId, LocalDateTime dateTime, TrafficStatus trafficStatus) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", locationId);

        Traffic traffic = instantiate(Traffic.class);
        ReflectionTestUtils.setField(traffic, "id", 1L);
        ReflectionTestUtils.setField(traffic, "location", location);
        ReflectionTestUtils.setField(traffic, "date", dateTime);
        ReflectionTestUtils.setField(traffic, "trafficStatus", trafficStatus);
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
