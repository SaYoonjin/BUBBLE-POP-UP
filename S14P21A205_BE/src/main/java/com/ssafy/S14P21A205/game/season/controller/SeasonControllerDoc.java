package com.ssafy.S14P21A205.game.season.controller;

import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonRankingsResponse;
import com.ssafy.S14P21A205.game.season.dto.CurrentSeasonTopRankingsResponse;
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
}
