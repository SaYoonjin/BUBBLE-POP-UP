package com.ssafy.S14P21A205.auth.dto;

/** 용도: JWT access/refresh 토큰 응답 DTO. */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {
}
