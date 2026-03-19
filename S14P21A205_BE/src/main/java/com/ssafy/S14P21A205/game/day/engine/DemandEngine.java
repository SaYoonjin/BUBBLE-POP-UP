package com.ssafy.S14P21A205.game.day.engine;

import java.util.List;

public class DemandEngine {

    public long calculateDemandUnits(List<Integer> purchaseList, int purchaseCursor) {
        if (purchaseList == null || purchaseList.isEmpty() || purchaseCursor <= 0) {
            return 0L;
        }

        long demandUnits = 0L;
        int cursor = Math.min(purchaseCursor, purchaseList.size());
        for (int index = 0; index < cursor; index++) {
            demandUnits += purchaseList.get(index);
        }
        return demandUnits;
    }
}
