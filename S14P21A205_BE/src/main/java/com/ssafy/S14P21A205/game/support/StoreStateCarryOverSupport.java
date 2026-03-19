package com.ssafy.S14P21A205.game.support;

import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StoreStateCarryOverSupport {

    public static final int INITIAL_CAPITAL = 10_000_000;
    private static final BigDecimal INTERIOR_RATE = new BigDecimal("0.10");

    private StoreStateCarryOverSupport() {
    }

    public static int resolveInitialBalance(Store store) {
        return INITIAL_CAPITAL - resolveJoinInteriorCharge(store);
    }

    public static int resolveInitialStock() {
        return 0;
    }

    private static int resolveJoinInteriorCharge(Store store) {
        Location location = store == null ? null : store.getLocation();
        if (location == null || location.getRent() == null) {
            return 0;
        }
        return BigDecimal.valueOf(location.getRent())
                .multiply(INTERIOR_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
