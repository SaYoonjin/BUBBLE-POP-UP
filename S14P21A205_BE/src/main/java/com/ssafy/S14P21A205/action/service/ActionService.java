package com.ssafy.S14P21A205.action.service;

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

public interface ActionService {

    ActionStatusResponse getActionStatus(Integer userId);

    PromotionPriceResponse getPromotionPrices();

    ActionResponse executePromotion(Integer userId, PromotionRequest request);

    DiscountResponse executeDiscount(Integer userId, DiscountRequest request);

    DonationResponse executeDonation(Integer userId, DonationRequest request);

    EmergencyOrderResponse executeEmergencyOrder(Integer userId, EmergencyOrderRequest request);
}
