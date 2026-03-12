package com.ssafy.S14P21A205.user.controller;

import com.ssafy.S14P21A205.auth.dto.AuthMeResponse;
import com.ssafy.S14P21A205.exception.ErrorResponse;
import com.ssafy.S14P21A205.user.dto.UserNicknameUpdateRequest;
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

@Tag(name = "User API", description = "사용자 정보 API")
public interface UserControllerDoc {

    @Operation(summary = "사용자 조회", description = "인증된 사용자가 자신의 사용자 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AuthMeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "미인증 또는 접근 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthMeResponse> getUser(
            @Parameter(description = "조회할 사용자 ID")
            String userId,
            @Parameter(hidden = true) Authentication authentication
    );

    @Operation(summary = "닉네임 변경", description = "인증된 사용자가 자신의 닉네임을 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = AuthMeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthMeResponse> updateMyNickname(
            @Parameter(description = "변경할 사용자 ID")
            String userId,
            UserNicknameUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication
    );
}
