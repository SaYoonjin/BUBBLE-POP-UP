package com.ssafy.S14P21A205.auth.service;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.auth.dto.AuthTokenResponse;
import com.ssafy.S14P21A205.config.SsafyOAuthSettings;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 용도: 인증 플로우 시작/내 정보 조립 서비스. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_REGISTRATION_ID = "google";

    private final AuthRedirectService authRedirectService;
    private final UserService userService;
    private final SsafyOAuthSettings ssafyOAuthSettings;
    private final JwtTokenService jwtTokenService;

    /** 용도: OAuth2 로그인 시작 URI 생성. */
    public URI startLogin(String provider, String redirect, HttpServletRequest request) {
        authRedirectService.storeLoginRedirect(request, redirect);
        return URI.create("/oauth2/authorization/" + resolveRegistrationId(provider));
    }

    /** 용도: 현재 인증 사용자 정보를 응답 DTO로 변환. */
    public AuthMeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        var user = userService.getCurrentUser(authentication);
        return AuthMeResponse.from(authentication, user);
    }

    /** 용도: refresh token으로 JWT 재발급. */
    public AuthTokenResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        return jwtTokenService.refresh(refreshToken);
    }

    /** 용도: refresh token 폐기 및 세션 정리. */
    public void logout(String refreshToken, HttpServletRequest request) {
        jwtTokenService.revoke(refreshToken);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private String resolveRegistrationId(String provider) {
        if (!StringUtils.hasText(provider)) {
            return DEFAULT_REGISTRATION_ID;
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if ("google".equals(normalized)) {
            return normalized;
        }
        if ("ssafy".equals(normalized) && ssafyOAuthSettings.isConfigured()) {
            return normalized;
        }
        if ("ssafy".equals(normalized)) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
        }
        throw new BaseException(ErrorCode.INVALID_INPUT_VALUE);
    }
}
