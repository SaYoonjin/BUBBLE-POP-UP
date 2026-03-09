package com.ssafy.S14P21A205.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserTests {

    @Test
    void usesProvidedNicknameWhenAvailable() {
        User user = new User("user@example.com", "ssafy-user");

        assertEquals("ssafy-user", user.getNickname());
        assertEquals(User.UserRole.GENERAL, user.getRole());
    }

    @Test
    void truncatesLongNicknameToThirtyCharacters() {
        User user = new User("user@example.com", "123456789012345678901234567890-extra");

        assertEquals("123456789012345678901234567890", user.getNickname());
        assertEquals(30, user.getNickname().length());
    }

    @Test
    void fallsBackToGeneratedNicknameWhenBlank() {
        User user = new User("user@example.com", "   ");

        assertNotNull(user.getNickname());
        assertFalse(user.getNickname().isBlank());
        assertTrue(user.getNickname().length() <= 30);
    }
}
