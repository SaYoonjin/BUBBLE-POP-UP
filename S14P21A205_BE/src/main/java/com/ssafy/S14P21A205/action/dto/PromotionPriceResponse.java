package com.ssafy.S14P21A205.action.dto;

import com.ssafy.S14P21A205.action.entity.Action;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "홍보 가격 조회 응답")
public record PromotionPriceResponse(
        @Schema(description = "홍보 종류별 가격 목록") List<PromotionItem> promotion
) {
    @Schema(description = "홍보 항목")
    public record PromotionItem(
            @Schema(description = "홍보 종류") PromotionType promotionType,
            @Schema(description = "홍보 비용") int promotionPrice
    ) {
    }

    public static PromotionPriceResponse from(List<Action> actions) {
        List<PromotionItem> items = actions.stream()
                .map(a -> new PromotionItem(a.getPromotionType(), a.getCost()))
                .toList();
        return new PromotionPriceResponse(items);
    }
}
