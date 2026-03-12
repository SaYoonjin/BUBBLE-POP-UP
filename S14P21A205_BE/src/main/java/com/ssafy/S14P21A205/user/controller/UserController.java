package com.ssafy.S14P21A205.user.controller;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.user.dto.UserNicknameUpdateRequest;
import com.ssafy.S14P21A205.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserControllerDoc {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Override
    public ResponseEntity<AuthMeResponse> getUser(
            @PathVariable String userId,
            Authentication authentication
    ) {
        var user = userService.getUser(userId, authentication);
        return ResponseEntity.ok(AuthMeResponse.from(authentication, user));
    }

    @PatchMapping("/{userId}/nickname")
    @Override
    public ResponseEntity<AuthMeResponse> updateMyNickname(
            @PathVariable String userId,
            @Valid @RequestBody UserNicknameUpdateRequest request,
            Authentication authentication
    ) {
        var user = userService.changeNickname(userId, authentication, request.nickname());
        return ResponseEntity.ok(AuthMeResponse.from(authentication, user));
    }
}
