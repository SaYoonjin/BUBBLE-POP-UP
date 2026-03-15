package com.ssafy.S14P21A205.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "Server error occurred."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "Invalid input value."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "Requested resource was not found."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "Authentication is required."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002", "Access is denied."),
    INVALID_DAY(HttpStatus.BAD_REQUEST, "GAME-001", "Invalid day value."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME-002", "Requested day report was not found."),
    NOT_PARTICIPATING(HttpStatus.FORBIDDEN, "GAME-003", "No participating season is available."),
    SHOP_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP-001", "Shop item was not found."),
    SHOP_ITEM_ALREADY_PURCHASED(HttpStatus.CONFLICT, "SHOP-002", "Shop item is already purchased."),
    SHOP_INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "SHOP-003", "Insufficient points."),
    SHOP_STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP-004", "Store was not found.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
