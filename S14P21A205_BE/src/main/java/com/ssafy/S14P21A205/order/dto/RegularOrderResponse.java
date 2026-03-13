package com.ssafy.S14P21A205.order.dto;

import lombok.Builder;

@Builder
public record RegularOrderResponse(
        Long orderId,
        Integer menuId,
        Integer quantity,
        Integer costPrice,
        Integer totalCost,
        Float discount
) {
}