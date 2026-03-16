package com.ssafy.S14P21A205.action.controller;

import com.ssafy.S14P21A205.action.dto.ActionResponse;
import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.DiscountRequest;
import com.ssafy.S14P21A205.action.dto.DiscountResponse;
import com.ssafy.S14P21A205.action.dto.DonationRequest;
import com.ssafy.S14P21A205.action.dto.DonationResponse;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderRequest;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.dto.PromotionRequest;
import com.ssafy.S14P21A205.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@Tag(name = "Action", description = "게임 액션 API")
public interface ActionControllerDoc {

    @Operation(summary = "액션 사용 현황 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ActionStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "가게/시즌 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ActionStatusResponse> getActionStatus(Authentication authentication);

    @Operation(summary = "홍보 가격 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PromotionPriceResponse.class)))
    })
    ResponseEntity<PromotionPriceResponse> getPromotionPrices();

    @Operation(summary = "홍보 실행", description = "INFLUENCER/SNS/LEAFLET/FRIEND 중 선택하여 홍보 실행",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홍보 실행 성공"),
            @ApiResponse(responseCode = "400", description = "잔액 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "오늘 이미 사용한 액션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ActionResponse> executePromotion(Authentication authentication, PromotionRequest request);

    @Operation(summary = "할인 이벤트 실행",
            description = "할인율(%) 또는 할인금액(원) 지정. 판매가 영구 변경. 평균가 대비 가격 구간에 따라 유입률 배수 적용.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "할인 실행 성공",
                    content = @Content(schema = @Schema(implementation = DiscountResponse.class))),
            @ApiResponse(responseCode = "400", description = "잔액 부족 / 잘못된 입력 (음수, 0%, 100% 이상 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "오늘 이미 사용한 액션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DiscountResponse> executeDiscount(Authentication authentication, DiscountRequest request);

    @Operation(summary = "나눔 이벤트 실행",
            description = "나눔 수량 지정 (1~50개). 5개 단위로 유입률 보너스 +0.01. 재고 즉시 차감.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "나눔 실행 성공",
                    content = @Content(schema = @Schema(implementation = DonationResponse.class))),
            @ApiResponse(responseCode = "400", description = "재고 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "오늘 이미 사용한 액션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<DonationResponse> executeDonation(Authentication authentication, DonationRequest request);

    @Operation(summary = "긴급 발주 실행",
            description = "수량 지정하여 긴급 발주. 비용 = 원가 × 수량 × 1.5. 30초 후 도착.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "긴급 발주 접수 성공",
                    content = @Content(schema = @Schema(implementation = EmergencyOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "잔액 부족",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "오늘 이미 사용한 액션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<EmergencyOrderResponse> executeEmergencyOrder(Authentication authentication, EmergencyOrderRequest request);
}
