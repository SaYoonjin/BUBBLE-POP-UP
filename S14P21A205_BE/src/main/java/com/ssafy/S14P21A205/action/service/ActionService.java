package com.ssafy.S14P21A205.action.service;

import com.ssafy.S14P21A205.action.dto.ActionStatusResponse;
import com.ssafy.S14P21A205.action.dto.PromotionPriceResponse;

public interface ActionService {

    ActionStatusResponse getActionStatus(Integer userId);

    PromotionPriceResponse getPromotionPrices();
}
