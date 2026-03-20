package com.ssafy.S14P21A205.game.day.controller;

import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.game.day.dto.GameDayReportResponse;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.dto.GameStateResponse;
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

@Tag(name = "Game Day API", description = "Game day APIs")
public interface GameDayControllerDoc {

    @Operation(
            summary = "Get the current day opening info",
            description = "Return the current day's opening information without requiring a manual start trigger.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Day opening info success",
                    content = @Content(schema = @Schema(implementation = GameDayStartResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Active season or day data not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<GameDayStartResponse> startDay(@Parameter(hidden = true) Authentication authentication);

    @Operation(
            summary = "Get the current business state",
            description = "Calculate and return the authoritative game state from Redis state.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Game state success",
                    content = @Content(schema = @Schema(implementation = GameStateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Active season or game day state not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<GameStateResponse> getGameState(@Parameter(hidden = true) Authentication authentication);

    @Operation(
            summary = "Get a day report",
            description = "Return the stored business report for the requested day.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Day report success",
                    content = @Content(schema = @Schema(implementation = GameDayReportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid day",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not participating in a season",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Day report not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<GameDayReportResponse> getDayReport(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "Day number to read (1-7)") Integer day
    );
}
