package com.ssafy.S14P21A205.game.day.controller;

import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartRequest;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
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

@Tag(name = "Game Day API", description = "Game day start API")
public interface GameDayControllerDoc {

    @Operation(
            summary = "n번째 날의 영업을 시작합니다.",
            description = "Initialize today's business state for the active season.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Day start success",
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
    ResponseEntity<GameDayStartResponse> startDay(
            GameDayStartRequest request,
            @Parameter(hidden = true) Authentication authentication
    );
}
