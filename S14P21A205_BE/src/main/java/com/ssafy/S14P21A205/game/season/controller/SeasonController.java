package com.ssafy.S14P21A205.game.season.controller;

import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.SeasonSummaryResponse;
import com.ssafy.S14P21A205.game.season.service.SeasonRankingService;
import com.ssafy.S14P21A205.game.season.service.SeasonSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game")
public class SeasonController implements SeasonControllerDoc {

    private final SeasonRankingService seasonRankingService;
    private final SeasonSummaryService seasonSummaryService;

    @Override
    @GetMapping("/seasons/current/rankings/top")
    public ResponseEntity<CurrentSeasonTopRankingsResponse> getCurrentTopRankings(Authentication authentication) {
        return ResponseEntity.ok(seasonRankingService.getCurrentTopRankings());
    }

    @Override
    @GetMapping("/seasons/current/rankings/final")
    public ResponseEntity<CurrentSeasonRankingsResponse> getCurrentFinalRankings(Authentication authentication) {
        return ResponseEntity.ok(seasonRankingService.getCurrentFinalRankings(authentication));
    }

    // seasonId -> 특정 시즌 조회 (없으면 가장 최근 종료 시즌)
    // userId -> 특정 유저 조회 (없으면 로그인 유저)
    @Override
    @GetMapping("/seasons/summary")
    public ResponseEntity<SeasonSummaryResponse> getSeasonSummary(
            Authentication authentication,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Integer userId
    ) {
        return ResponseEntity.ok(seasonSummaryService.getSeasonSummary(authentication, seasonId, userId));
    }
}