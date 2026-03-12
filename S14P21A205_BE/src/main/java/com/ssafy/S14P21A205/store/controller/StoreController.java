package com.ssafy.S14P21A205.store.controller;

import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.service.StoreService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController implements StoreControllerDoc {

    private final StoreService storeService;

    // 내 매장 조회
    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<StoreResponse> getStore(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(storeService.getStore(userId));
    }

    // 팝업 이전
    @Override
    @PatchMapping("/location")
    public ResponseEntity<UpdateStoreLocationResponse> updateStoreLocation(
            @RequestBody UpdateStoreLocationRequest request
    ) {
        // TODO: 실제 로그인 사용자 UUID 추출 로직으로 교체 필요
        UUID userId = UUID.fromString("e0552239-7d1c-4084-befa-4ec8dc9f4a12");

        return ResponseEntity.ok(storeService.updateStoreLocation(userId, request));
    }
}