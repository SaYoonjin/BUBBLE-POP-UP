package com.ssafy.S14P21A205.shop.dto;

import java.util.List;
import lombok.Builder;

/**
 * 구매한 아이템 목록 응답 DTO
 */
@Builder
public record PurchasedItemListResponse(
        List<PurchasedItemResponse> items
) {
    public static PurchasedItemListResponse of(List<PurchasedItemResponse> items) {
        return PurchasedItemListResponse.builder()
                .items(items)
                .build();
    }
}