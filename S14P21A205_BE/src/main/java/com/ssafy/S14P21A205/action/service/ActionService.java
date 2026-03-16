package com.ssafy.S14P21A205.action.service;

import com.ssafy.S14P21A205.action.dto.ActionResponse;
import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderRequest;
import com.ssafy.S14P21A205.action.dto.EmergencyOrderResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;
import com.ssafy.S14P21A205.action.dto.PromotionRequest;

public interface ActionService {

    ActionStatusResponse getActionStatus(Integer userId);

    PromotionPriceResponse getPromotionPrices();

    ActionResponse executePromotion(Integer userId, PromotionRequest request);

    ActionResponse executeDiscount(Integer userId);

    ActionResponse executeDonation(Integer userId);

    EmergencyOrderResponse executeEmergencyOrder(Integer userId, EmergencyOrderRequest request);
}
