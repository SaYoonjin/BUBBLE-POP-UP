package com.ssafy.S14P21A205.action.controller;

import com.ssafy.S14P21A205.action.dto.ActionResponse;
import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.DiscountRequest;
import com.ssafy.S14P21A205.action.dto.DiscountResponse;
import com.ssafy.S14P21A205.action.dto.DonationRequest;
import com.ssafy.S14P21A205.action.dto.DonationResponse;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderRequest;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.dto.PromotionRequest;
import com.ssafy.S14P21A205.action.service.ActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/promotion")
    @Override
    public ResponseEntity<ActionResponse> executePromotion(
            Authentication authentication,
            @Valid @RequestBody PromotionRequest request
    ) {
        Integer userId = Integer.parseInt(authentication.getName());
        return ResponseEntity.ok(actionService.executePromotion(userId, request));
    }

    @PostMapping("/discount")
    @Override
    public ResponseEntity<DiscountResponse> executeDiscount(
            Authentication authentication,
            @Valid @RequestBody DiscountRequest request
    ) {
        Integer userId = Integer.parseInt(authentication.getName());
        return ResponseEntity.ok(actionService.executeDiscount(userId, request));
    }

    @PostMapping("/donation")
    @Override
    public ResponseEntity<DonationResponse> executeDonation(
            Authentication authentication,
            @Valid @RequestBody DonationRequest request
    ) {
        Integer userId = Integer.parseInt(authentication.getName());
        return ResponseEntity.ok(actionService.executeDonation(userId, request));
    }

    @PostMapping("/emergency-order")
    @Override
    public ResponseEntity<EmergencyOrderResponse> executeEmergencyOrder(
            Authentication authentication,
            @Valid @RequestBody EmergencyOrderRequest request
    ) {
        Integer userId = Integer.parseInt(authentication.getName());
        return ResponseEntity.ok(actionService.executeEmergencyOrder(userId, request));
    }
}
