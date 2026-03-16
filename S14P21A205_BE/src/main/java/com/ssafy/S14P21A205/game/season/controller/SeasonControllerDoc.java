package com.ssafy.S14P21A205.game.season.controller;

import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.SeasonSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Game Season API", description = "Season ranking API")
public interface SeasonControllerDoc {

    @Operation(
            summary = "Get current top rankings",
            description = "Return the current top 10 realtime rankings from Redis.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Top ranking lookup success",
                    content = @Content(schema = @Schema(implementation = CurrentSeasonTopRankingsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Top ranking cache not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CurrentSeasonTopRankingsResponse> getCurrentTopRankings(
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "Get finalized season rankings",
            description = "Return the finalized season rankings from SQL, including top 10 and the requesting user's ranking.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Final ranking lookup success",
                    content = @Content(schema = @Schema(implementation = CurrentSeasonRankingsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Final ranking data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<CurrentSeasonRankingsResponse> getCurrentFinalRankings(
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(
            summary = "Get finished season summary",
            description = "Return the finished season summary for the specified user. If seasonId is omitted, the latest finished season is used. If userId is omitted, the authenticated user is used.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Season summary lookup success",
                    content = @Content(schema = @Schema(implementation = SeasonSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Season summary data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<SeasonSummaryResponse> getSeasonSummary(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "Finished season ID. If omitted, the latest finished season is used.", example = "12")
            @RequestParam(required = false) Long seasonId,
            @Parameter(description = "Target user ID. If omitted, the authenticated user is used.", example = "27")
            @RequestParam(required = false) Integer userId
    );
}
