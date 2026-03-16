package com.ssafy.S14P21A205.store.dto;

public record LocationResponse(
        Long locationId,
        String locationName,
        Integer rent,
        Integer interiorCost,
        Float discount
) {
}
