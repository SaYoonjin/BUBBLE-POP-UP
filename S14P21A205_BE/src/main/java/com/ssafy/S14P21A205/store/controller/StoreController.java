package com.ssafy.S14P21A205.store.controller;

import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.MenuListResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerDoc {

    private final StoreService storeService;

    @Override
    @GetMapping
    public ResponseEntity<StoreResponse> getStore(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(storeService.getStore(userId));
    }

    @Override
    @PatchMapping("/location")
    public ResponseEntity<UpdateStoreLocationResponse> updateStoreLocation(
            Authentication authentication,
            @RequestBody UpdateStoreLocationRequest request
    ) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(storeService.updateStoreLocation(userId, request));
    }

    @Override
    @GetMapping("/locations")
    public ResponseEntity<LocationListResponse> getLocations(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(storeService.getLocations(userId));
    }

    @Override
    @GetMapping("/menus")
    public ResponseEntity<MenuListResponse> getMenus(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(storeService.getMenus(userId));
    }

    private Integer extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }

        String rawUserId = authentication.getName();
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new RuntimeException("userId가 없습니다.");
        }

        try {
            return Integer.valueOf(rawUserId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("userId 형식이 올바르지 않습니다.");
        }
    }
}
