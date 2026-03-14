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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonRankingService {

    private final SeasonRankingRedisRepository seasonRankingRedisRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonRankingRecordRepository seasonRankingRecordRepository;

    /**
     * 현재 진행 중 시즌의 실시간 TOP 랭킹 조회
     *
     * 흐름:
     * 1. Redis에서 실시간 TOP 랭킹 캐시 조회
     * 2. 캐시가 없으면 빈 응답 생성
     * 3. rank 기준으로 정렬 / 비정상 데이터 제거
     * 4. 최대 10개까지만 반환
     */
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

    /**
     * 가장 최근 종료된 시즌의 최종 랭킹 조회
     *
     * 흐름:
     * 1. FINISHED 상태의 가장 최근 시즌 조회
     * 2. 해당 시즌의 최종 랭킹 레코드를 DB에서 조회
     * 3. DB 엔티티를 응답 DTO로 변환
     * 4. TOP10 랭킹 추출
     * 5. 요청한 유저의 내 랭킹 조회
     * 6. TOP10 + 내 랭킹을 함께 반환
     */
    public CurrentSeasonRankingsResponse getCurrentFinalRankings(Integer userId) {
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

    /**
     * Redis에서 읽은 실시간 랭킹 목록을 정리한다.
     *
     * 역할: null 데이터 제거 / rank 없는 데이터 제거 / userId 없는 데이터 제거
     */
    private List<CurrentSeasonTopRankingItemResponse> normalizeTopRankings(List<CurrentSeasonTopRankingItemResponse> rankings) {
        if (rankings == null || rankings.isEmpty()) {
            return List.of();
        }

        return rankings.stream()
                .filter(ranking -> ranking != null && ranking.rank() != null && ranking.userId() != null)
                .sorted(Comparator.comparing(CurrentSeasonTopRankingItemResponse::rank))
                .toList();
    }

    /**
     * Redis에 실시간 TOP 랭킹이 없을 때 사용할 기본 응답 생성
     *
     * 흐름:
     * 1. 현재 진행 중 시즌이 있으면 seasonId만 채움
     * 2. 랭킹 목록은 빈 리스트
     * 3. refreshedAt은 null
     */
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

    // 최종 랭킹 전체에서 TOP10만 추출
    private List<CurrentSeasonRankingItemResponse> extractTopTenRanks(List<CurrentSeasonRankingItemResponse> allRankings) {
        return allRankings.stream()
                .filter(ranking -> ranking.rank() != null && ranking.rank() <= 10)
                .toList();
    }

    // 요청한 유저의 랭킹 한 건 찾기
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

