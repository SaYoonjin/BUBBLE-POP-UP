package com.ssafy.S14P21A205.shop.controller;

import com.ssafy.S14P21A205.shop.dto.PurchasedItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Shop", description = "상점 API")
public interface ShopControllerDoc {

    @Operation(
            summary = "상점 아이템 목록 조회",
            description = "상점에서 구매 가능한 아이템 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<ShopItemListResponse> getShopItems();

    @Operation(
            summary = "구매 아이템 조회",
            description = "현재 시즌에서 구매한 아이템 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<PurchasedItemListResponse> getPurchasedItems(
            @Parameter(hidden = true) Authentication authentication
    );
}