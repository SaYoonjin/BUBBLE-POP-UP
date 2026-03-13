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

    /**
     * 현재 판매가 조회 api
     * userId로 매장을 조회한 뒤 현재 판매 메뉴의 가격/재고 정보를 반환
     */
    @Override
    @GetMapping
    public ResponseEntity<CurrentOrderResponse> getCurrentOrder(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return ResponseEntity.ok(orderService.getCurrentOrder(userId));
    }

    // Authentication 객체에서 userId 추출
    private Integer extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다.");
        }
        return Integer.valueOf(authentication.getName());
    }
}
