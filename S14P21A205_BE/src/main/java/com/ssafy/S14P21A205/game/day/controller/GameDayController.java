package com.ssafy.S14P21A205.game.day.controller;

import com.ssafy.S14P21A205.game.day.dto.GameDayStartRequest;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.service.GameDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/day")
public class GameDayController implements GameDayControllerDoc {

    private final GameDayService gameDayService;

    @PostMapping("/start")
    @Override
    public ResponseEntity<GameDayStartResponse> startDay(
            @Valid @RequestBody GameDayStartRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gameDayService.startDay(authentication, request));
    }
}
