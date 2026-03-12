package com.ssafy.S14P21A205.store.controller;

import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerDoc {

    private final StoreService storeService;

    // 내 매장 조회
    @Override
    @GetMapping
    public ResponseEntity<StoreResponse> getStore(
            @AuthenticationPrincipal(expression = "storeId") Long storeId
    ) {
        return ResponseEntity.ok(storeService.getStore(storeId));
    }

    // 팝업 이전
    @Override
    @PatchMapping("/location")
    public ResponseEntity<UpdateStoreLocationResponse> updateStoreLocation(
            @AuthenticationPrincipal(expression = "storeId") Long storeId,
            @RequestBody UpdateStoreLocationRequest request
    ) {
        return ResponseEntity.ok(storeService.updateStoreLocation(storeId, request));
    }

    // 지역 목록 조회
    @Override
    @GetMapping("/locations")
    public ResponseEntity<LocationListResponse> getLocations(
            @AuthenticationPrincipal(expression = "storeId") Long storeId
    ) {
        return ResponseEntity.ok(storeService.getLocations(storeId));
    }
}