package com.ssafy.S14P21A205.game.season.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.season.dto.SeasonJoinRequest;
import com.ssafy.S14P21A205.game.season.dto.SeasonJoinResponse;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRecordRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.lang.reflect.Constructor;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SeasonJoinServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private SeasonRankingRecordRepository seasonRankingRecordRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SeasonJoinService seasonJoinService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        seasonJoinService = new SeasonJoinService(
                userService,
                seasonRepository,
                seasonRankingRecordRepository,
                storeRepository,
                locationRepository,
                menuRepository,
                stringRedisTemplate
        );
    }

    @Test
    void joinCurrentSeasonStoresInitialCaptureRateForNewSeasonStore() {
        User user = user(7);
        Season season = season(11L);
        Location location = location(3L, 100_000);
        Menu menu = menu(5L, 2_000);
        Store savedStore = store(21L, user, season, location, menu, 2_000);

        when(userService.getCurrentUser(any(Authentication.class))).thenReturn(user);
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));
        when(storeRepository.findFirstByUser_IdAndSeason_IdOrderByIdDesc(7, 11L)).thenReturn(Optional.empty());
        when(locationRepository.findById(3L)).thenReturn(Optional.of(location));
        when(storeRepository.findFirstByUser_IdOrderBySeason_IdDescIdDesc(7)).thenReturn(Optional.empty());
        when(menuRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(menu));
        when(storeRepository.save(any(Store.class))).thenReturn(savedStore);

        SeasonJoinResponse response = seasonJoinService.joinCurrentSeason(
                org.mockito.Mockito.mock(Authentication.class),
                new SeasonJoinRequest(3, "테스트매장")
        );

        assertThat(response.storeId()).isEqualTo(21L);
        assertThat(response.balance()).isEqualTo(9_990_000);
        verify(valueOperations).set("balance:21", "9990000");
        verify(valueOperations).set("stock:21", "0");
        verify(valueOperations).set("captureRate:21", "0.10");
    }

    private User user(Integer id) {
        User user = new User("join-%d@test.com".formatted(id), "join-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Season season(Long id) {
        Season season = instantiate(Season.class);
        ReflectionTestUtils.setField(season, "id", id);
        ReflectionTestUtils.setField(season, "status", SeasonStatus.IN_PROGRESS);
        return season;
    }

    private Location location(Long id, Integer rent) {
        Location location = instantiate(Location.class);
        ReflectionTestUtils.setField(location, "id", id);
        ReflectionTestUtils.setField(location, "rent", rent);
        return location;
    }

    private Menu menu(Long id, Integer originPrice) {
        Menu menu = instantiate(Menu.class);
        ReflectionTestUtils.setField(menu, "id", id);
        ReflectionTestUtils.setField(menu, "originPrice", originPrice);
        return menu;
    }

    private Store store(Long id, User user, Season season, Location location, Menu menu, Integer price) {
        Store store = instantiate(Store.class);
        ReflectionTestUtils.setField(store, "id", id);
        ReflectionTestUtils.setField(store, "user", user);
        ReflectionTestUtils.setField(store, "season", season);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "menu", menu);
        ReflectionTestUtils.setField(store, "storeName", "fixture-store");
        ReflectionTestUtils.setField(store, "price", price);
        return store;
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
