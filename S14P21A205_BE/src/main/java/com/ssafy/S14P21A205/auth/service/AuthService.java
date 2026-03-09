package com.ssafy.S14P21A205.auth.service;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.config.SsafyOAuthSettings;
import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    /** 용도: OAuth2 로그인 시작 URI 생성. */
    public URI startLogin(String provider, String redirect, HttpServletRequest request) {
        authRedirectService.storeLoginRedirect(request, redirect);
        return URI.create("/oauth2/authorization/" + resolveRegistrationId(provider));
    }

    /** 용도: 현재 인증 사용자 정보를 응답 DTO로 변환. */
    public AuthMeResponse me(OAuth2AuthenticationToken authenticationToken) {
        OAuth2User oauth2User = authenticationToken == null ? null : authenticationToken.getPrincipal();
        if (oauth2User == null || !StringUtils.hasText(extractEmail(oauth2User))) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        var user = userService.upsertFromAuthentication(authenticationToken);
        return AuthMeResponse.from(authenticationToken, user);
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

    private String extractEmail(OAuth2User oauth2User) {
        if (oauth2User == null) {
            return null;
        }
        Object email = oauth2User.getAttribute("email");
        return email == null ? null : String.valueOf(email);
    }
}
