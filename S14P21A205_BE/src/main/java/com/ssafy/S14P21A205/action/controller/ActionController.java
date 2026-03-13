package com.ssafy.S14P21A205.action.controller;

import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.service.ActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/actions")
public class ActionController implements ActionControllerDoc {

    private final ActionService actionService;

    @GetMapping("/status")
    @Override
    public ResponseEntity<ActionStatusResponse> getActionStatus(Authentication authentication) {
        Integer userId = Integer.parseInt(authentication.getName());
        return ResponseEntity.ok(actionService.getActionStatus(userId));
    }

    @GetMapping("/promotion/price")
    @Override
    public ResponseEntity<PromotionPriceResponse> getPromotionPrices() {
        return ResponseEntity.ok(actionService.getPromotionPrices());
    }
}