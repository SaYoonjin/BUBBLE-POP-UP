package com.ssafy.S14P21A205.user.controller;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.user.dto.UserNicknameUpdateRequest;
import com.ssafy.S14P21A205.user.dto.UserPointsResponse;
import com.ssafy.S14P21A205.user.dto.UserRecordsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "User API", description = "User API")
public interface UserControllerDoc {

    @Operation(
            summary = "Get user",
            description = "Returns the authenticated user's profile.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(schema = @Schema(implementation = AuthMeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthMeResponse> getUser(Authentication authentication);

    @Operation(
            summary = "Get points",
            description = "Returns the authenticated user's current points.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(schema = @Schema(implementation = UserPointsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserPointsResponse> getUserPoints(Authentication authentication);

    @Operation(
            summary = "Get records",
            description = "Returns up to 10 recent finished season records for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(schema = @Schema(implementation = UserRecordsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<UserRecordsResponse> getUserRecords(Authentication authentication);

    @Operation(
            summary = "Update nickname",
            description = "Updates the authenticated user's nickname.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(schema = @Schema(implementation = AuthMeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthMeResponse> updateMyNickname(
            UserNicknameUpdateRequest request,
            Authentication authentication
    );
}
