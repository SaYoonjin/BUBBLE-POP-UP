package com.ssafy.S14P21A205.shop.controller;

import com.ssafy.S14P21A205.shop.dto.PurchaseItemsRequest;
import com.ssafy.S14P21A205.shop.dto.PurchaseItemsResponse;
import com.ssafy.S14P21A205.shop.dto.PurchasedItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

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

    @Operation(
            summary = "아이템 일괄 구매",
            description = "선택한 상점 아이템들을 포인트로 일괄 구매합니다. 같은 카테고리 아이템은 함께 구매할 수 없으며, 전체 포인트가 부족하면 전체 구매가 실패합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    ResponseEntity<PurchaseItemsResponse> purchaseItems(
            PurchaseItemsRequest request,
            @Parameter(hidden = true) Authentication authentication
    );
}
