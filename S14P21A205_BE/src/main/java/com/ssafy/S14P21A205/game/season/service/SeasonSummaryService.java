package com.ssafy.S14P21A205.game.season.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.dto.SeasonSummaryResponse;
import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonRankingRecord;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.game.season.repository.SeasonRankingRecordRepository;
import com.ssafy.S14P21A205.game.season.repository.SeasonRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonSummaryService {

    private static final BigDecimal ZERO_DECIMAL = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

    private final SeasonRepository seasonRepository;
    private final SeasonRankingRecordRepository seasonRankingRecordRepository;
    private final UserService userService;

    public SeasonSummaryResponse getSeasonSummary(Authentication authentication, Long seasonId, Integer targetUserId) {
        Integer requesterUserId = userService.getCurrentUser(authentication).getId();
        Season season = resolveFinishedSeason(seasonId);
        Integer resolvedUserId = targetUserId == null ? requesterUserId : targetUserId;

        SeasonRankingRecord record = seasonRankingRecordRepository
                .findFirstByStore_Season_IdAndStore_User_IdOrderByIdDesc(season.getId(), resolvedUserId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));

        return toResponse(record);
    }

    private Season resolveFinishedSeason(Long seasonId) {
        if (seasonId == null) {
            return seasonRepository.findFirstByStatusOrderByIdDesc(SeasonStatus.FINISHED)
                    .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        return seasonRepository.findByIdAndStatus(seasonId, SeasonStatus.FINISHED)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private SeasonSummaryResponse toResponse(SeasonRankingRecord record) {
        Store store = record.getStore();

        return new SeasonSummaryResponse(
                store.getSeason().getId(),
                Boolean.TRUE.equals(record.getIsBankruptcy()) ? null : record.getFinalRank(),
                new SeasonSummaryResponse.StoreInfo(
                        store.getStoreName(),
                        store.getLocation().getLocationName(),
                        store.getMenu().getMenuName()
                ),
                new SeasonSummaryResponse.BusinessRecord(
                        valueOf(record.getTotalRevenue()),
                        valueOf(record.getTotalCost()),
                        valueOf(record.getTotalNetProfit()),
                        defaultInt(record.getTotalVisitors()),
                        toOneDecimal(record.getRoi()),
                        defaultInt(record.getDaysPlayed())
                )
        );
    }

    private Double toOneDecimal(BigDecimal value) {
        if (value == null) {
            return ZERO_DECIMAL.doubleValue();
        }
        return value.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private Double toOneDecimal(Float value) {
        if (value == null) {
            return ZERO_DECIMAL.doubleValue();
        }
        return BigDecimal.valueOf(value.doubleValue()).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
