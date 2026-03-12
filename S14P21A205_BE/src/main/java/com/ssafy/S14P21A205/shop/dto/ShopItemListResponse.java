package com.ssafy.S14P21A205.shop.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShopItemListResponse {

    private List<ShopItemResponse> items;

    public static ShopItemListResponse of(List<ShopItemResponse> items) {
        return ShopItemListResponse.builder()
                .items(items)
                .build();
    }
}