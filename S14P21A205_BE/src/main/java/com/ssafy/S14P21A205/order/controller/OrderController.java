package com.ssafy.S14P21A205.order.controller;

import com.ssafy.S14P21A205.order.dto.CurrentOrderResponse;
import com.ssafy.S14P21A205.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController implements OrderControllerDoc {

    private final OrderService orderService;

    @Override
    @GetMapping
    public ResponseEntity<CurrentOrderResponse> getCurrentOrder(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(orderService.getCurrentOrder(userId));
    }

    private Integer extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        return Integer.valueOf(authentication.getName());
    }
}
