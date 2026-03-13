package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.order.dto.CurrentOrderResponse;

public interface OrderService {
    CurrentOrderResponse getCurrentOrder(Integer userId);
}