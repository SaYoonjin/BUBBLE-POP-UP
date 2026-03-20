package com.ssafy.S14P21A205.auth.controller;

import com.ssafy.S14P21A205.auth.dto.AuthLogoutRequest;
import com.ssafy.S14P21A205.auth.dto.AuthTokenRefreshRequest;
import com.ssafy.S14P21A205.auth.dto.AuthTokenResponse;
import com.ssafy.S14P21A205.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 용도: 인증 API 엔드포인트 구현. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerDoc {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;

    /** 용도: OAuth2 로그인 시작. */
    @GetMapping("/login")
    @Override
    public ResponseEntity<Void> login(
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpServletRequest request
    ) {
        URI location = authService.startLogin(provider, redirect, request);
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    /** 용도: refresh token으로 access/refresh 토큰 재발급. */
    @PostMapping("/refresh")
    @Override
    public ResponseEntity<AuthTokenResponse> refresh(
            @RequestBody(required = false) AuthTokenRefreshRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        AuthTokenResponse tokenResponse = authService.refresh(resolveRefreshToken(httpServletRequest, request));
        writeRefreshTokenCookie(httpServletRequest, httpServletResponse, tokenResponse.refreshToken(), tokenResponse.refreshExpiresIn());
        return ResponseEntity.ok(maskRefreshToken(tokenResponse));
    }

    /** 용도: refresh token 폐기 및 로컬 로그아웃. */
    @PostMapping("/logout")
    @Override
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) AuthLogoutRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        authService.logout(resolveRefreshToken(httpServletRequest, request), httpServletRequest);
        clearRefreshTokenCookie(httpServletRequest, httpServletResponse);
        return ResponseEntity.noContent().build();
    }

    private String resolveRefreshToken(HttpServletRequest request, AuthTokenRefreshRequest body) {
        String cookieValue = extractRefreshTokenCookie(request);
        if (cookieValue != null && !cookieValue.isBlank()) {
            return cookieValue;
        }
        return body == null ? null : body.refreshToken();
    }

    private String resolveRefreshToken(HttpServletRequest request, AuthLogoutRequest body) {
        String cookieValue = extractRefreshTokenCookie(request);
        if (cookieValue != null && !cookieValue.isBlank()) {
            return cookieValue;
        }
        return body == null ? null : body.refreshToken();
    }

    private String extractRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshTokenCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String refreshToken,
            long refreshExpiresIn
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        boolean secure = isSecureRequest(request);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(refreshExpiresIn))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = isSecureRequest(request);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private AuthTokenResponse maskRefreshToken(AuthTokenResponse tokenResponse) {
        return new AuthTokenResponse(
                tokenResponse.accessToken(),
                null,
                tokenResponse.tokenType(),
                tokenResponse.expiresIn(),
                tokenResponse.refreshExpiresIn()
        );
    }
}
