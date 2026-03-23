package com.ssafy.S14P21A205.game.support;

import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;

public final class StoreStateCarryOverSupport {

    public static final int INITIAL_CAPITAL = 5_000_000;

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
        if (location == null || location.getInteriorCost() == null) {
            return 0;
        }
        return location.getInteriorCost();
    }
}
