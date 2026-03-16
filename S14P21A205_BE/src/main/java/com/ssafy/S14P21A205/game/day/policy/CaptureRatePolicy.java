package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.season.entity.DailyReport;
import com.ssafy.S14P21A205.game.season.repository.DailyReportRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaptureRatePolicy {

    private static final BigDecimal INITIAL_CAPTURE_RATE = new BigDecimal("0.00");
    private static final BigDecimal DECIMAL_ZERO = new BigDecimal("0.00");

    private final DailyReportRepository dailyReportRepository;

    public BigDecimal resolveStartingCaptureRate(Store store, int day) {
        if (day == 1) {
            return INITIAL_CAPTURE_RATE;
        }

        DailyReport previousDay = dailyReportRepository.findByStoreIdAndDay(store.getId(), day - 1)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));
        return normalizeScale(previousDay.getCaptureRate());
    }

    private BigDecimal normalizeScale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal resolveInflowRate(BigDecimal baseCaptureRate, BigDecimal actionCaptureRateBoost) {
        BigDecimal resolved = baseCaptureRate == null ? DECIMAL_ZERO : baseCaptureRate;
        if (actionCaptureRateBoost != null) {
            resolved = resolved.add(actionCaptureRateBoost);
        }
        return resolved.max(DECIMAL_ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
