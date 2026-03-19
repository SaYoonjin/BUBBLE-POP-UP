package com.ssafy.S14P21A205.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTests {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private ItemUserRepository itemUserRepository;

    @Mock
    private GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

    private StoreServiceImpl storeService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-09T05:33:00Z"), ZoneId.of("Asia/Seoul"));
        storeService = new StoreServiceImpl(
                storeRepository,
                locationRepository,
                menuRepository,
                itemUserRepository,
                gameDayStoreStateRedisRepository,
                fixedClock
        );
    }

    @Test
    void updateStoreLocationReservesNextDayMoveAndChargesDepositImmediately() {
        Store store = store(15L, 3L, 2, 7);
        Location targetLocation = location(4L, "Gangnam", 200_000, 120_000);
        GameDayLiveState state = new GameDayLiveState(0L, 0L, 50, LocalDateTime.of(2026, 3, 9, 14, 33, 0));

        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));
        when(locationRepository.findById(4L)).thenReturn(Optional.of(targetLocation));
        when(gameDayStoreStateRedisRepository.findBalance(15L, 2)).thenReturn(Optional.of(1_000_000L));
        when(gameDayStoreStateRedisRepository.find(15L, 2)).thenReturn(Optional.of(state));

        UpdateStoreLocationResponse response = storeService.updateStoreLocation(1, new UpdateStoreLocationRequest(4L));

        assertThat(response.locationId()).isEqualTo(4L);
        assertThat(response.balance()).isEqualTo(800_000);
        assertThat(store.getLocation().getId()).isEqualTo(3L);
        assertThat(store.getPendingLocation().getId()).isEqualTo(4L);
        assertThat(store.getPendingLocationApplyDay()).isEqualTo(3);
        verify(gameDayStoreStateRedisRepository).saveBalance(15L, 2, 800_000L);
        verify(gameDayStoreStateRedisRepository).updateField(15L, 2, "location_change_cost", "200000");
    }

    @Test
    void updateStoreLocationRejectsWhenNextDayMoveIsAlreadyReserved() {
        Store store = store(15L, 3L, 2, 7);
        ReflectionTestUtils.setField(store, "pendingLocation", location(4L, "Gangnam", 200_000, 120_000));
        ReflectionTestUtils.setField(store, "pendingLocationReservedDay", 2);
        ReflectionTestUtils.setField(store, "pendingLocationApplyDay", 3);

        when(storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(1, SeasonStatus.IN_PROGRESS))
                .thenReturn(Optional.of(store));

        assertThatThrownBy(() -> storeService.updateStoreLocation(1, new UpdateStoreLocationRequest(5L)))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    private Store store(Long storeId, Long locationId, int currentDay, int totalDays) {
        Location location = location(locationId, "Seongsu", 100_000, 150_000);

        Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", 9L);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(season, "currentDay", currentDay);
        ReflectionTestUtils.setField(season, "totalDays", totalDays);
        LocalDateTime now = LocalDateTime.of(2026, 3, 9, 14, 33, 0);
        LocalDateTime seasonStartAt = now.minusSeconds(120L + (currentDay - 1L) * 180L + 170L);
        ReflectionTestUtils.setField(season, "startTime", seasonStartAt);
        ReflectionTestUtils.setField(season, "endTime", seasonStartAt.plusSeconds(120L + totalDays * 180L + 120L));

        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "season", season);
        return store;
    }

    private Location location(Long id, String name, int interiorCost, int rent) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "locationName", name);
        ReflectionTestUtils.setField(location, "interiorCost", interiorCost);
        ReflectionTestUtils.setField(location, "rent", rent);
        return location;
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
