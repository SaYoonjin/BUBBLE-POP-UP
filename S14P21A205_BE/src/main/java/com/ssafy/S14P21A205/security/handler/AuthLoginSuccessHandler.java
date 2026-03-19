package com.ssafy.S14P21A205.security.handler;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.auth.dto.AuthTokenResponse;
import com.ssafy.S14P21A205.auth.service.AuthRedirectService;
import com.ssafy.S14P21A205.auth.service.JwtTokenService;
import com.ssafy.S14P21A205.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** 용도: OAuth2 로그인 성공 시 안전한 경로로 리다이렉트. */
@Component
public class AuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthRedirectService authRedirectService;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final String defaultRedirectUrl;

    /** 용도: 성공 핸들러 초기화. */
    public AuthLoginSuccessHandler(
            AuthRedirectService authRedirectService,
            JwtTokenService jwtTokenService,
            UserService userService,
            @Value("${app.auth.default-redirect-url:/auth/callback}") String defaultRedirectUrl
    ) {
        this.authRedirectService = authRedirectService;
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
        this.defaultRedirectUrl = defaultRedirectUrl;
    }

    /** 용도: 성공 시 리다이렉트 수행. */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        AuthTokenResponse tokenResponse = null;
        if (authentication instanceof OAuth2AuthenticationToken oauth2AuthenticationToken) {
            var user = userService.upsertFromAuthentication(oauth2AuthenticationToken);
            tokenResponse = jwtTokenService.issueTokens(
                    user,
                    AuthMeResponse.from(oauth2AuthenticationToken, user)
            );
        }
        String redirect = authRedirectService.consumeLoginRedirect(request);
        String target = authRedirectService.isSafeRedirect(redirect) ? redirect.trim() : defaultRedirectUrl;
        if (!authRedirectService.isSafeRedirect(target)) {
            target = "/auth/callback";
        }
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        addRefreshTokenCookie(request, response, tokenResponse);
        response.sendRedirect(appendTokens(target.trim(), tokenResponse));
    }

    private String appendTokens(String target, AuthTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return target;
        }
        String base = target;
        int hashIndex = base.indexOf('#');
        if (hashIndex >= 0) {
            base = base.substring(0, hashIndex);
        }
        return base
                + "#accessToken=" + encode(tokenResponse.accessToken())
                + "&tokenType=" + encode(tokenResponse.tokenType());
    }

    private void addRefreshTokenCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthTokenResponse tokenResponse
    ) {
        if (tokenResponse == null || tokenResponse.refreshToken() == null) {
            return;
        }
        boolean secure = isSecureRequest(request);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(tokenResponse.refreshExpiresIn()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
