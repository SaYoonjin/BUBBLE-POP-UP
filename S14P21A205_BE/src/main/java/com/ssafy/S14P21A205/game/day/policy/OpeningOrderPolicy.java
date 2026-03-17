package com.ssafy.S14P21A205.game.day.policy;

import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import java.util.Optional;

public class OpeningOrderPolicy {

    public Optional<Order> resolve(OrderRepository orderRepository, Long storeId, int day) {
        return orderRepository.findDailyStartOrder(storeId, day);
    }
}
