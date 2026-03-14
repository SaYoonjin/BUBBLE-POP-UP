package com.ssafy.S14P21A205.game.season.controller;

import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
import com.ssafy.S14P21A205.game.season.service.SeasonRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/game")
public class SeasonController implements SeasonControllerDoc {

    private final SeasonRankingService seasonRankingService;

    // 현재 진행 중 시즌의 실시간 TOP 랭킹 조회
    @Override
    @GetMapping("/seasons/current/rankings/top")
    public ResponseEntity<CurrentSeasonTopRankingsResponse> getCurrentTopRankings(Authentication authentication) {
        return ResponseEntity.ok(seasonRankingService.getCurrentTopRankings());
    }

    // 시즌 마감 후 랭킹 조회 (TOP10 + 내 순위)
    @Override
    @GetMapping("/seasons/current/rankings/final")
    public ResponseEntity<CurrentSeasonRankingsResponse> getCurrentFinalRankings(Authentication authentication) {
        Integer userId = Integer.valueOf(authentication.getName());
        return ResponseEntity.ok(seasonRankingService.getCurrentFinalRankings(userId));
    }
}
