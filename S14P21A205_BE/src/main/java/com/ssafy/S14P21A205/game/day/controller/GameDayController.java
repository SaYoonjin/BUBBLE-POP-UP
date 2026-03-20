package com.ssafy.S14P21A205.game.day.controller;

import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
import com.ssafy.S14P21A205.game.day.service.GameDayReportService;
import com.ssafy.S14P21A205.game.day.service.GameDayStartService;
import com.ssafy.S14P21A205.game.day.service.GameDayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game/day")
public class GameDayController implements GameDayControllerDoc {

    private final GameDayReportService gameDayReportService;
    private final GameDayStartService gameDayStartService;
    private final GameDayStateService gameDayStateService;

    @GetMapping("/start")
    @Override
    public ResponseEntity<GameDayStartResponse> startDay(Authentication authentication) {
        return ResponseEntity.ok(gameDayStartService.startDay(authentication));
    }

    @GetMapping("/state")
    @Override
    public ResponseEntity<GameStateResponse> getGameState(Authentication authentication) {
        return ResponseEntity.ok(gameDayStateService.getGameState(authentication));
    }

    @GetMapping("/reports/{day}")
    @Override
    public ResponseEntity<GameDayReportResponse> getDayReport(
            Authentication authentication,
            @PathVariable Integer day
    ) {
        return ResponseEntity.ok(gameDayReportService.getDayReport(authentication, day));
    }
}
