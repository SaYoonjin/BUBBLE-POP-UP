package com.ssafy.S14P21A205.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SsafyOAuthSettingsTests {

    @Test
    void allowsCompletelyDisabledConfiguration() {
        assertDoesNotThrow(() -> new SsafyOAuthSettings("", "", "", "", ""));
    }

    @Test
    void allowsCompleteConfiguration() {
        assertDoesNotThrow(() -> new SsafyOAuthSettings(
                "client-id",
                "client-secret",
                "https://project.ssafy.com/oauth/sso-check",
                "https://project.ssafy.com/ssafy/oauth2/token",
                "https://project.ssafy.com/ssafy/resources/userInfo"
        ));
    }

    @Test
    void rejectsPartialConfiguration() {
        assertThrows(IllegalStateException.class, () -> new SsafyOAuthSettings(
                "client-id",
                "",
                "https://project.ssafy.com/oauth/sso-check",
                "",
                "https://project.ssafy.com/ssafy/resources/userInfo"
        ));
    }
}
