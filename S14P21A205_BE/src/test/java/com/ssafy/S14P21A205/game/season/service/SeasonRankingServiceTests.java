package com.ssafy.S14P21A205.game.season.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingItemResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingItemResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonRankingRecord;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRecordRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRedisRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class SeasonRankingServiceTests {

    private final SeasonRankingRedisRepository seasonRankingRedisRepository = mock(SeasonRankingRedisRepository.class);
    private final SeasonRepository seasonRepository = mock(SeasonRepository.class);
    private final SeasonRankingRecordRepository seasonRankingRecordRepository = mock(SeasonRankingRecordRepository.class);
    private final UserService userService = mock(UserService.class);

    private final SeasonRankingService seasonRankingService = new SeasonRankingService(
            seasonRankingRedisRepository,
            seasonRepository,
            seasonRankingRecordRepository,
            userService
    );

    @Test
    void getCurrentTopRankingsReturnsTopTenFromRedis() {
        CurrentSeasonTopRankingsResponse cachedResponse = new CurrentSeasonTopRankingsResponse(
                3L,
                buildRankingItems(12),
                "2026-03-13T15:00:00"
        );

        when(seasonRankingRedisRepository.findCurrentTopRankings()).thenReturn(Optional.of(cachedResponse));

        CurrentSeasonTopRankingsResponse response = seasonRankingService.getCurrentTopRankings();

        assertEquals(3L, response.seasonId());
        assertEquals(10, response.rankings().size());
        assertEquals(1, response.rankings().get(0).rank());
        assertEquals("2026-03-13T15:00:00", response.refreshedAt());
    }

    @Test
    void getCurrentFinalRankingsReturnsTopTenAndMyRankingFromSql() {
        Integer myUserId = 200;
        Authentication authentication = authenticate(myUserId, "me");
        Season season = mock(Season.class);
        when(season.getId()).thenReturn(12L);
        when(season.getStatus()).thenReturn(SeasonStatus.FINISHED);

        List<SeasonRankingRecord> finalizedRecords = buildFinalizedRecords(myUserId);

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.FINISHED)).thenReturn(Optional.of(season));
        when(seasonRankingRecordRepository.findByStore_Season_IdOrderByFinalRankAsc(12L)).thenReturn(finalizedRecords);

        CurrentSeasonRankingsResponse response = seasonRankingService.getCurrentFinalRankings(authentication);

        assertEquals(12L, response.seasonId());
        assertEquals(10, response.rankings().size());
        assertEquals(12, response.myRanking().rank());
        assertEquals("me", response.myRanking().nickname());
        assertFalse(response.rankings().stream().anyMatch(ranking -> ranking.userId().equals(myUserId)));
    }

    @Test
    void getCurrentFinalRankingsIncludesAllTiedUsersWithinTopTenRanks() {
        Integer myUserId = 999;
        Authentication authentication = authenticate(myUserId, "me");
        Season season = mock(Season.class);
        when(season.getId()).thenReturn(12L);
        when(season.getStatus()).thenReturn(SeasonStatus.FINISHED);

        List<SeasonRankingRecord> finalizedRecords = buildFinalizedRecordsWithTiedEighth(myUserId);

        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.FINISHED)).thenReturn(Optional.of(season));
        when(seasonRankingRecordRepository.findByStore_Season_IdOrderByFinalRankAsc(12L)).thenReturn(finalizedRecords);

        CurrentSeasonRankingsResponse response = seasonRankingService.getCurrentFinalRankings(authentication);

        assertEquals(12L, response.seasonId());
        assertEquals(12, response.rankings().size());
        assertEquals(8, response.rankings().get(7).rank());
        assertEquals(8, response.rankings().get(11).rank());
        assertTrue(response.rankings().stream().allMatch(ranking -> ranking.rank() <= 10));
        assertEquals(13, response.myRanking().rank());
    }

    @Test
    void getCurrentTopRankingsReturnsEmptyWhenTopCacheDoesNotExist() {
        Season season = mock(Season.class);
        when(season.getId()).thenReturn(3L);
        when(seasonRankingRedisRepository.findCurrentTopRankings()).thenReturn(Optional.empty());
        when(seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)).thenReturn(Optional.of(season));

        CurrentSeasonTopRankingsResponse response = seasonRankingService.getCurrentTopRankings();

        assertEquals(3L, response.seasonId());
        assertTrue(response.rankings().isEmpty());
        assertNull(response.refreshedAt());
    }

    private Authentication authenticate(Integer userId, String nickname) {
        Authentication authentication = mock(Authentication.class);
        when(userService.getCurrentUser(authentication)).thenReturn(createUser(userId, nickname));
        return authentication;
    }

    private List<CurrentSeasonTopRankingItemResponse> buildRankingItems(int count) {
        List<CurrentSeasonTopRankingItemResponse> rankings = new ArrayList<>();
        for (int rank = 1; rank <= count; rank++) {
            rankings.add(new CurrentSeasonTopRankingItemResponse(
                    rank,
                    rank,
                    "user-" + rank,
                    "store-" + rank,
                    BigDecimal.valueOf(100 - rank).setScale(1),
                    rank * 100000L,
                    rank == 1 ? 100 : rank == 2 ? 50 : rank == 3 ? 30 : 0
            ));
        }
        return rankings;
    }

    private List<SeasonRankingRecord> buildFinalizedRecordsWithTiedEighth(Integer myUserId) {
        List<SeasonRankingRecord> records = new ArrayList<>();
        for (int rank = 1; rank <= 7; rank++) {
            records.add(createFinalizedRecord(rank, rank, "user-" + rank, rank * 100_000));
        }
        for (int userId = 8; userId <= 12; userId++) {
            records.add(createFinalizedRecord(8, userId, "user-" + userId, userId * 100_000));
        }
        records.add(createFinalizedRecord(13, myUserId, "me", 50_000));
        return records;
    }

    private List<SeasonRankingRecord> buildFinalizedRecords(Integer myUserId) {
        List<SeasonRankingRecord> records = new ArrayList<>();
        for (int rank = 1; rank <= 11; rank++) {
            records.add(createFinalizedRecord(rank, rank, "user-" + rank, rank * 100_000));
        }
        records.add(createFinalizedRecord(12, myUserId, "me", 50_000));
        return records;
    }

    private SeasonRankingRecord createFinalizedRecord(int rank, Integer userId, String nickname, int totalRevenue) {
        SeasonRankingRecord record = mock(SeasonRankingRecord.class);
        Store store = createStore(
                Long.valueOf(rank),
                userId,
                nickname,
                nickname + "-store",
                "location-" + rank,
                "menu-" + rank,
                10
        );

        when(record.getFinalRank()).thenReturn(rank);
        when(record.getStore()).thenReturn(store);
        when(record.getRoi()).thenReturn((float) (100 - rank));
        when(record.getTotalRevenue()).thenReturn(totalRevenue);
        when(record.getRewardPoints()).thenReturn(rank == 1 ? 50 : rank == 2 ? 30 : rank == 3 ? 20 : 0);
        return record;
    }

    private Store createStore(
            Long storeId,
            Integer userId,
            String nickname,
            String storeName,
            String locationName,
            String menuName,
            Integer originPrice
    ) {
        User user = createUser(userId, nickname);

        Location location = BeanUtils.instantiateClass(Location.class);
        ReflectionTestUtils.setField(location, "id", storeId + 1);
        ReflectionTestUtils.setField(location, "locationName", locationName);
        ReflectionTestUtils.setField(location, "rent", 1000);

        Menu menu = BeanUtils.instantiateClass(Menu.class);
        ReflectionTestUtils.setField(menu, "id", storeId + 10);
        ReflectionTestUtils.setField(menu, "menuName", menuName);
        ReflectionTestUtils.setField(menu, "originPrice", originPrice);

        Store store = BeanUtils.instantiateClass(Store.class);
        ReflectionTestUtils.setField(store, "id", storeId);
        ReflectionTestUtils.setField(store, "user", user);
        ReflectionTestUtils.setField(store, "location", location);
        ReflectionTestUtils.setField(store, "menu", menu);
        ReflectionTestUtils.setField(store, "storeName", storeName);
        ReflectionTestUtils.setField(store, "price", originPrice * 2);
        return store;
    }

    private User createUser(Integer userId, String nickname) {
        User user = new User(userId + "@example.com", nickname);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}