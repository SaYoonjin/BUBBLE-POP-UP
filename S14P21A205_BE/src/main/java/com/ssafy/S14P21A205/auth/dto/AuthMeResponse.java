package com.ssafy.S14P21A205.auth.dto;

import com.ssafy.S14P21A205.user.entity.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** 용도: 현재 로그인 사용자 정보 응답 DTO. */
public record AuthMeResponse(
        String subject,
        String provider,
        String email,
        String name,
        String picture,
        String nickname,
        String role
) {

    /** 용도: OAuth2 사용자 객체를 DTO로 변환. */
    public static AuthMeResponse from(OAuth2User oauth2User) {
        return from(oauth2User, null, null);
    }

    /** 용도: OAuth2 정보와 DB 사용자 정보를 함께 담아 반환. */
    public static AuthMeResponse from(OAuth2User oauth2User, User user) {
        return from(oauth2User, null, user);
    }

    /** 용도: 인증 토큰과 DB 사용자 정보를 함께 담아 반환. */
    public static AuthMeResponse from(OAuth2AuthenticationToken authenticationToken, User user) {
        if (authenticationToken == null) {
            return null;
        }
        return from(
                authenticationToken.getPrincipal(),
                authenticationToken.getAuthorizedClientRegistrationId(),
                user
        );
    }

    private static AuthMeResponse from(OAuth2User oauth2User, String provider, User user) {
        if (oauth2User == null) {
            return null;
        }
        return new AuthMeResponse(
                extractSubject(oauth2User),
                provider,
                asString(oauth2User.getAttribute("email")),
                asString(oauth2User.getAttribute("name")),
                asString(oauth2User.getAttribute("picture")),
                user == null ? null : user.getNickname(),
                (user == null || user.getRole() == null) ? null : user.getRole().name()
        );
    }

    private static String extractSubject(OAuth2User oauth2User) {
        String userId = asString(oauth2User.getAttribute("userId"));
        if (userId != null) {
            return userId;
        }
        String sub = asString(oauth2User.getAttribute("sub"));
        if (sub != null) {
            return sub;
        }
        String id = asString(oauth2User.getAttribute("id"));
        if (id != null) {
            return id;
        }
        return asString(oauth2User.getName());
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
