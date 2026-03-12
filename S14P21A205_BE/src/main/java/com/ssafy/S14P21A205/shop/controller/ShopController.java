package com.ssafy.S14P21A205.shop.controller;

import com.ssafy.S14P21A205.shop.dto.PurchasedItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import com.ssafy.S14P21A205.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("shop")
@RequiredArgsConstructor
public class ShopController implements ShopControllerDoc {

    private final ShopService shopService;

    @Override
    @GetMapping("/items")
    public ResponseEntity<ShopItemListResponse> getShopItems() {
        return ResponseEntity.ok(shopService.getShopItems());
    }

    @GetMapping("/purchased")
    public ResponseEntity<PurchasedItemListResponse> getPurchasedItems(
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(shopService.getPurchasedItems(userId));
    }
}