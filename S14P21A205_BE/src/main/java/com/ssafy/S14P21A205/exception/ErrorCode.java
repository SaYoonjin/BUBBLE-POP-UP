package com.ssafy.S14P21A205.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 용도: API 공통 에러 코드/상태/메시지 관리. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "잘못된 요청 값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "요청한 자원을 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002", "접근 권한이 없습니다."),

    SHOP_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP-001", "상점 아이템을 찾을 수 없습니다."),
    SHOP_ITEM_ALREADY_PURCHASED(HttpStatus.CONFLICT, "SHOP-002", "이미 구매한 아이템입니다."),
    SHOP_INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "SHOP-003", "포인트가 부족합니다."),
    SHOP_STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP-004", "매장을 찾을 수 없습니다."),

    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORE-001", "가게를 찾을 수 없습니다."),
    GAME_STATE_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME-001", "게임 상태를 찾을 수 없습니다."),
    SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME-002", "진행 중인 시즌이 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME-003", "메뉴를 찾을 수 없습니다."),

    ACTION_ALREADY_USED(HttpStatus.CONFLICT, "ACTION-001", "오늘 이미 사용한 액션입니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "ACTION-002", "잔액이 부족합니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "ACTION-003", "재고가 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
