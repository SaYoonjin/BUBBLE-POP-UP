package com.ssafy.S14P21A205.store.dto;

public record StoreResponse(
        String location,
        String popupName,
        String menu,
        Integer day
) {
}