package com.ssafy.S14P21A205.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record PurchaseItemsResponse(
        List<PurchaseItemResponse> purchasedItems,
        @JsonProperty("UsedPoints") Integer usedPoints,
        Integer remainingPoints
) {
    public static PurchaseItemsResponse of(
            List<PurchaseItemResponse> purchasedItems,
            Integer usedPoints,
            Integer remainingPoints
    ) {
        return PurchaseItemsResponse.builder()
                .purchasedItems(purchasedItems)
                .usedPoints(usedPoints)
                .remainingPoints(remainingPoints)
                .build();
    }
}
