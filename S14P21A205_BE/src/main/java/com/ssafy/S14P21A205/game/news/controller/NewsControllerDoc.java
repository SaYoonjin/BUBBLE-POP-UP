package com.ssafy.S14P21A205.game.news.controller;

import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.game.news.dto.NewsListResponse;
import com.ssafy.S14P21A205.game.news.dto.NewsRankingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "News API", description = "뉴스 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface NewsControllerDoc {

    @Operation(summary = "오늘의 뉴스 조회", description = "현재 진행 중인 시즌의 지정된 Day에 해당하는 뉴스 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NewsListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "진행 중인 시즌이 없거나 해당 Day의 뉴스가 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<NewsListResponse> getTodayNews(@PathVariable int day);

    @Operation(summary = "지역별 매출/유동인구 순위 조회", description = "현재 시즌의 지정된 Day 기준 지역별 매출 순위와 유동인구 순위를 전일대비 증감율과 함께 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = NewsRankingResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "시즌이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<NewsRankingResponse> getAreaRankings(@PathVariable int day);
}
