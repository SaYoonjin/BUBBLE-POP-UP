package com.ssafy.S14P21A205.auth.dto;

/** 용도: refresh token 재발급 요청 DTO. */
public record AuthTokenRefreshRequest(
        String refreshToken
) {
}
