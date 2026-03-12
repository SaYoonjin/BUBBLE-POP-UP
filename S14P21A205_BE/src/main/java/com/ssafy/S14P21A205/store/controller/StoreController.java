package com.ssafy.S14P21A205.store.controller;

import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerDoc {

    private final StoreService storeService;

    @Override
    @GetMapping
    public ResponseEntity<StoreResponse> getStore(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(storeService.getStore(userId));
    }

    @Override
    @PatchMapping("/location")
    public ResponseEntity<UpdateStoreLocationResponse> updateStoreLocation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateStoreLocationRequest request
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(storeService.updateStoreLocation(userId, request));
    }

    @Override
    @GetMapping("/locations")
    public ResponseEntity<LocationListResponse> getLocations(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(storeService.getLocations(userId));
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");

        if (userIdClaim == null) {
            throw new RuntimeException("JWT에 userId 클레임이 없습니다.");
        }

        if (userIdClaim instanceof Long value) {
            return value;
        }

        if (userIdClaim instanceof Integer value) {
            return value.longValue();
        }

        if (userIdClaim instanceof String value) {
            return Long.valueOf(value);
        }

        throw new RuntimeException("JWT의 userId 클레임 형식이 올바르지 않습니다.");
    }
}