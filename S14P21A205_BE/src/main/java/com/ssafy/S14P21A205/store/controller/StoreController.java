package com.ssafy.S14P21A205.store.controller;

import com.ssafy.S14P21A205.store.dto.StoreResponse;
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
}
