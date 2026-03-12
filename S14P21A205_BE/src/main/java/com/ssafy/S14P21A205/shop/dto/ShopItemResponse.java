package com.ssafy.S14P21A205.shop.dto;

import com.ssafy.S14P21A205.shop.entity.Item;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShopItemResponse {

    private Long itemId;
    private String itemName;
    private String category;
    private Integer point;
    private BigDecimal discountRate;

    public static ShopItemResponse from(Item item) {
        return ShopItemResponse.builder()
                .itemId(item.getId())
                .itemName(item.getItemName())
                .category(item.getCategory().name())
                .point(item.getPoint())
                .discountRate(item.getDiscountRate())
                .build();
    }
}