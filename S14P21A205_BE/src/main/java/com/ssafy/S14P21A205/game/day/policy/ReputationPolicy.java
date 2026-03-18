package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.game.day.state.GameDayLiveState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ReputationPolicy {

    private static final BigDecimal ZERO_CAPTURE_RATE = new BigDecimal("0.00");
    private static final BigDecimal ZERO_REPUTATION = new BigDecimal("0.0");
    private static final BigDecimal MAX_REPUTATION = new BigDecimal("5.0");
    private static final BigDecimal REPUTATION_MULTIPLIER = new BigDecimal("5");

    public BigDecimal resolveCaptureRate(GameDayLiveState state) {
        if (state.captureRate() != null) {
            return state.captureRate();
        }
        if (state.startResponse() != null && state.startResponse().captureRate() != null) {
            return state.startResponse().captureRate();
        }
        return ZERO_CAPTURE_RATE;
    }

    public BigDecimal normalizeCaptureRate(BigDecimal value) {
        if (value == null) {
            return ZERO_CAPTURE_RATE;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal toReputationScore(BigDecimal captureRate) {
        BigDecimal score = captureRate.multiply(REPUTATION_MULTIPLIER).setScale(1, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO_REPUTATION;
        }
        if (score.compareTo(MAX_REPUTATION) > 0) {
            return MAX_REPUTATION;
        }
        return score;
    }

    public BigDecimal toReputationChange(BigDecimal captureRateDelta) {
        return captureRateDelta.multiply(REPUTATION_MULTIPLIER).setScale(1, RoundingMode.HALF_UP);
    }
}
