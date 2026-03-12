package com.ssafy.S14P21A205.auth.controller;

import com.ssafy.S14P21A205.auth.dto.AuthLogoutRequest;
import com.ssafy.S14P21A205.auth.dto.AuthTokenRefreshRequest;
import com.ssafy.S14P21A205.auth.dto.AuthTokenResponse;
import com.ssafy.S14P21A205.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

/** 용도: 인증 API Swagger 문서 정의. */
@Tag(name = "Auth API", description = "Google/SSAFY OAuth2 + JWT 인증 API")
public interface AuthControllerDoc {

    @Operation(
            summary = "OAuth2 로그인 시작",
            description = """
                    OAuth2 로그인 플로우를 시작합니다.

                    - provider 파라미터로 로그인 제공자(google|ssafy)를 선택할 수 있습니다.
                    - provider 미지정 시 google이 기본값입니다.
                    - redirect 파라미터로 로그인 완료 후 돌아갈 경로를 지정할 수 있습니다.
                    - 상대 경로(/...) 또는 allow-list에 등록된 절대 URL만 허용됩니다.
                    - 로그인 성공 후 redirect URL의 fragment(#...)로 accessToken을 전달합니다.
                    - refresh token은 HttpOnly 쿠키로 발급됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "선택한 OAuth2 인가 엔드포인트로 리다이렉트"),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 provider",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> login(
            @Parameter(description = "로그인 제공자", example = "google")
            String provider,
            @Parameter(description = "로그인 완료 후 이동 경로", example = "/swagger-ui/index.html")
            String redirect,
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(summary = "토큰 재발급", description = "HttpOnly refresh token 쿠키로 access/refresh 토큰을 회전 재발급합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = AuthTokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<AuthTokenResponse> refresh(
            AuthTokenRefreshRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest,
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletResponse httpServletResponse
    );

    @Operation(summary = "로그아웃", description = "refresh token 쿠키를 폐기하고 로컬 access token 사용을 종료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<Void> logout(
            AuthLogoutRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest,
            @Parameter(hidden = true) jakarta.servlet.http.HttpServletResponse httpServletResponse
    );
}
