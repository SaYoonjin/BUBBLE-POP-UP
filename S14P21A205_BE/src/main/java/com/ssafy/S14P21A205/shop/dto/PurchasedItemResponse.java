package com.ssafy.S14P21A205.shop.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 구매한 아이템 1건 응답 DTO
 */
@Builder
public record PurchasedItemResponse(
        Long itemId,
        BigDecimal discountRate
) {
    public static PurchasedItemResponse of(Long itemId, BigDecimal discountRate) {
        return PurchasedItemResponse.builder()
                .itemId(itemId)
                .discountRate(discountRate)
                .build();
    }
}