package com.ssafy.S14P21A205.user.controller;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.user.dto.UserNicknameUpdateRequest;
import com.ssafy.S14P21A205.user.dto.UserPointsResponse;
import com.ssafy.S14P21A205.user.dto.UserRecordsResponse;
import com.ssafy.S14P21A205.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserControllerDoc {

    private final UserService userService;

    @GetMapping
    @Override
    public ResponseEntity<AuthMeResponse> getUser(
            Authentication authentication
    ) {
        var user = userService.getUser(authentication);
        return ResponseEntity.ok(AuthMeResponse.from(authentication, user));
    }

    @GetMapping("/points")
    @Override
    public ResponseEntity<UserPointsResponse> getUserPoints(
            Authentication authentication
    ) {
        var user = userService.getUser(authentication);
        return ResponseEntity.ok(new UserPointsResponse(user.getPoint()));
    }

    @PatchMapping("/nickname")
    @Override
    public ResponseEntity<AuthMeResponse> updateMyNickname(
            @Valid @RequestBody UserNicknameUpdateRequest request,
            Authentication authentication
    ) {
        var user = userService.changeNickname(authentication, request.nickname());
        return ResponseEntity.ok(AuthMeResponse.from(authentication, user));
    }

    @GetMapping("/records")
    @Override
    public ResponseEntity<UserRecordsResponse> getUserRecords(
            Authentication authentication
    ) {
        return ResponseEntity.ok(userService.getUserRecords(authentication));
    }
}
