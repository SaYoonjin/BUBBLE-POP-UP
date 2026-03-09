package com.ssafy.S14P21A205.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 용도: SSAFY OAuth 설정 존재 여부를 판단하고 누락을 조기 검출. */
@Component
public class SsafyOAuthSettings {

    private final boolean configured;

    public SsafyOAuthSettings(
            @Value("${SSAFY_CLIENT_ID:}") String clientId,
            @Value("${SSAFY_CLIENT_SECRET:}") String clientSecret,
            @Value("${SSAFY_AUTHORIZATION_URI:}") String authorizationUri,
            @Value("${SSAFY_TOKEN_URI:}") String tokenUri,
            @Value("${SSAFY_USER_INFO_URI:}") String userInfoUri
    ) {
        Map<String, String> requiredValues = new LinkedHashMap<>();
        requiredValues.put("SSAFY_CLIENT_ID", clientId);
        requiredValues.put("SSAFY_CLIENT_SECRET", clientSecret);
        requiredValues.put("SSAFY_AUTHORIZATION_URI", authorizationUri);
        requiredValues.put("SSAFY_TOKEN_URI", tokenUri);
        requiredValues.put("SSAFY_USER_INFO_URI", userInfoUri);

        boolean anyConfigured = requiredValues.values().stream().anyMatch(StringUtils::hasText);
        List<String> missingKeys = requiredValues.entrySet().stream()
                .filter(entry -> !StringUtils.hasText(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        if (anyConfigured && !missingKeys.isEmpty()) {
            throw new IllegalStateException("SSAFY OAuth configuration is incomplete. Missing: " + String.join(", ", missingKeys));
        }

        this.configured = anyConfigured;
    }

    public boolean isConfigured() {
        return configured;
    }
}
