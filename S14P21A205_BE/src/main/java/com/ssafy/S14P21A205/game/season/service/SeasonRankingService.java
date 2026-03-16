package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonMyRankingResponse;
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
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonRankingService {

    private final SeasonRankingRedisRepository seasonRankingRedisRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonRankingRecordRepository seasonRankingRecordRepository;
    private final UserService userService;

    public CurrentSeasonTopRankingsResponse getCurrentTopRankings() {
        CurrentSeasonTopRankingsResponse cachedResponse = seasonRankingRedisRepository.findCurrentTopRankings()
                .orElseGet(this::buildEmptyCurrentTopRankingsResponse);

        List<CurrentSeasonTopRankingItemResponse> rankings = normalizeTopRankings(cachedResponse.rankings())
                .stream()
                .limit(10)
                .toList();

        return new CurrentSeasonTopRankingsResponse(
                cachedResponse.seasonId(),
                rankings,
                cachedResponse.refreshedAt()
        );
    }

    public CurrentSeasonRankingsResponse getCurrentFinalRankings(Authentication authentication) {
        Integer userId = userService.getCurrentUser(authentication).getId();
        return getCurrentFinalRankings(userId);
    }

    private CurrentSeasonRankingsResponse getCurrentFinalRankings(Integer userId) {
        Season season = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.FINISHED)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));

        List<CurrentSeasonRankingItemResponse> allRankings = seasonRankingRecordRepository
                .findByStore_Season_IdOrderByFinalRankAsc(season.getId())
                .stream()
                .map(this::toRankingItem)
                .toList();
        if (allRankings.isEmpty()) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        CurrentSeasonRankingItemResponse myRanking = findMyRanking(userId, allRankings);
        return new CurrentSeasonRankingsResponse(
                season.getId(),
                extractTopTenRanks(allRankings),
                toMyRankingResponse(myRanking)
        );
    }

    private List<CurrentSeasonTopRankingItemResponse> normalizeTopRankings(List<CurrentSeasonTopRankingItemResponse> rankings) {
        if (rankings == null || rankings.isEmpty()) {
            return List.of();
        }

        return rankings.stream()
                .filter(ranking -> ranking != null && ranking.rank() != null && ranking.userId() != null)
                .sorted(Comparator.comparing(CurrentSeasonTopRankingItemResponse::rank))
                .toList();
    }

    private CurrentSeasonTopRankingsResponse buildEmptyCurrentTopRankingsResponse() {
        Long seasonId = seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.IN_PROGRESS)
                .map(Season::getId)
                .orElse(null);
        return new CurrentSeasonTopRankingsResponse(seasonId, List.of(), null);
    }

    private CurrentSeasonRankingItemResponse toRankingItem(SeasonRankingRecord record) {
        return new CurrentSeasonRankingItemResponse(
                record.getFinalRank(),
                record.getStore().getUser().getId(),
                record.getStore().getUser().getNickname(),
                record.getStore().getStoreName(),
                record.getStore().getLocation().getLocationName(),
                record.getStore().getMenu().getMenuName(),
                normalizeRoi(record.getRoi()),
                valueOf(record.getTotalRevenue()),
                record.getRewardPoints()
        );
    }

    private List<CurrentSeasonRankingItemResponse> extractTopTenRanks(List<CurrentSeasonRankingItemResponse> allRankings) {
        return allRankings.stream()
                .filter(ranking -> ranking.rank() != null && ranking.rank() <= 10)
                .toList();
    }

    private CurrentSeasonRankingItemResponse findMyRanking(
            Integer userId,
            List<CurrentSeasonRankingItemResponse> allRankings
    ) {
        return allRankings.stream()
                .filter(ranking -> ranking.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private CurrentSeasonMyRankingResponse toMyRankingResponse(CurrentSeasonRankingItemResponse ranking) {
        return new CurrentSeasonMyRankingResponse(
                ranking.rank(),
                ranking.nickname(),
                ranking.storeName(),
                ranking.locationName(),
                ranking.menuName(),
                ranking.roi(),
                ranking.totalRevenue(),
                ranking.rewardPoints()
        );
    }

    private BigDecimal normalizeRoi(Float roi) {
        if (roi == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(roi.doubleValue()).setScale(1, RoundingMode.HALF_UP);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}