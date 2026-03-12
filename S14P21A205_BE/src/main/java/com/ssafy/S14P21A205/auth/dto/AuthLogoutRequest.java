package com.ssafy.S14P21A205.auth.dto;

/** 용도: refresh token 폐기 요청 DTO. */
public record AuthLogoutRequest(
        String refreshToken
) {
}
