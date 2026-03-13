package com.ssafy.S14P21A205.shop.dto;

import java.util.List;

public record PurchaseItemsRequest(
        List<Long> itemId
) {
    public PurchaseItemsRequest {
        itemId = itemId == null ? List.of() : itemId;
    }
}
