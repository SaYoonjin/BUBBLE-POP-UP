package com.ssafy.S14P21A205.shop.dto;

import com.ssafy.S14P21A205.shop.entity.Item;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PurchaseItemResponse(
        Long itemId,
        String itemName,
        String category,
        BigDecimal discountRate,
        Integer usedPoints
) {
    public static PurchaseItemResponse of(Item item, BigDecimal discountRate) {
        return PurchaseItemResponse.builder()
                .itemId(item.getId())
                .itemName(item.getItemName())
                .category(item.getCategory().name())
                .discountRate(discountRate)
                .usedPoints(item.getPoint())
                .build();
    }
}
