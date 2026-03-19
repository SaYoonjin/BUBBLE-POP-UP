package com.ssafy.S14P21A205.game.day.engine;

import com.ssafy.S14P21A205.order.entity.Order;
import java.time.LocalDateTime;
import java.util.List;

public class EmergencyOrderEngine {

    public EmergencyOrderState resolve(List<Order> emergencyOrders, LocalDateTime effectiveNow) {
        int arrivedStock = 0;
        LocalDateTime pendingArriveAt = null;
        long totalCost = 0L;

        for (Order emergencyOrder : emergencyOrders) {
            totalCost += valueOf(emergencyOrder.getTotalCost());
            LocalDateTime arrivedTime = emergencyOrder.getArrivedTime();
            boolean arrived = Boolean.TRUE.equals(emergencyOrder.getIsArrived())
                    || (arrivedTime != null && !arrivedTime.isAfter(effectiveNow));
            if (arrived) {
                arrivedStock += emergencyOrder.getQuantity();
                if (!Boolean.TRUE.equals(emergencyOrder.getIsArrived())) {
                    emergencyOrder.markArrived();
                }
                continue;
            }

            if (pendingArriveAt == null
                    || (arrivedTime != null && pendingArriveAt != null && arrivedTime.isBefore(pendingArriveAt))
                    || (arrivedTime != null && pendingArriveAt == null)) {
                pendingArriveAt = arrivedTime;
            }
        }

        return new EmergencyOrderState(pendingArriveAt != null, pendingArriveAt, arrivedStock, totalCost);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    public record EmergencyOrderState(
            boolean pending,
            LocalDateTime arriveAt,
            int arrivedStock,
            long totalCost
    ) {
    }
}
