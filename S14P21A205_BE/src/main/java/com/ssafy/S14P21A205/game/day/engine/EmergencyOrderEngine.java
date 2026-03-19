package com.ssafy.S14P21A205.game.day.engine;

import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.shop.entity.Menu;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class EmergencyOrderEngine {

    public EmergencyOrderState resolve(List<Order> emergencyOrders, LocalDateTime effectiveNow) {
        LocalDateTime pendingArriveAt = null;
        Order latestArrivedOrder = null;

        for (Order emergencyOrder : emergencyOrders) {
            LocalDateTime arrivedTime = emergencyOrder.getArrivedTime();
            boolean arrived = Boolean.TRUE.equals(emergencyOrder.getIsArrived())
                    || (arrivedTime != null && !arrivedTime.isAfter(effectiveNow));
            if (arrived) {
                latestArrivedOrder = emergencyOrder;
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

        return new EmergencyOrderState(pendingArriveAt != null, pendingArriveAt, latestArrivedOrder);
    }

    public InventoryState applyArrivalsBetween(
            InventoryState baseState,
            List<Order> emergencyOrders,
            LocalDateTime fromExclusive,
            LocalDateTime toInclusive
    ) {
        InventoryState currentState = baseState;
        if (toInclusive == null) {
            return currentState;
        }

        for (Order emergencyOrder : emergencyOrders) {
            LocalDateTime arrivedTime = emergencyOrder.getArrivedTime();
            if (arrivedTime == null || arrivedTime.isAfter(toInclusive)) {
                continue;
            }
            if (fromExclusive != null && !arrivedTime.isAfter(fromExclusive)) {
                continue;
            }
            currentState = applyArrival(currentState, emergencyOrder);
            if (!Boolean.TRUE.equals(emergencyOrder.getIsArrived())) {
                emergencyOrder.markArrived();
            }
        }
        return currentState;
    }

    public Order resolveLatestArrivedOrderAt(List<Order> emergencyOrders, LocalDateTime effectiveNow) {
        if (effectiveNow == null) {
            return null;
        }

        Order latestArrivedOrder = null;
        for (Order emergencyOrder : emergencyOrders) {
            LocalDateTime arrivedTime = emergencyOrder.getArrivedTime();
            if (arrivedTime == null || arrivedTime.isAfter(effectiveNow)) {
                continue;
            }
            latestArrivedOrder = emergencyOrder;
        }
        return latestArrivedOrder;
    }

    public long resolveOrderedDayTotalCost(List<Order> emergencyOrders, int orderedDay) {
        long totalCost = 0L;
        for (Order emergencyOrder : emergencyOrders) {
            if (!Objects.equals(emergencyOrder.getOrderedDay(), orderedDay)) {
                continue;
            }
            totalCost += valueOf(emergencyOrder.getTotalCost());
        }
        return totalCost;
    }

    private InventoryState applyArrival(InventoryState currentState, Order emergencyOrder) {
        InventoryState safeState = currentState == null
                ? new InventoryState(null, 0, null)
                : currentState;

        Menu currentMenu = safeState.menu();
        Menu nextMenu = emergencyOrder.getMenu() == null ? currentMenu : emergencyOrder.getMenu();
        Integer nextSalePrice = emergencyOrder.getSalePrice() == null
                ? safeState.salePrice()
                : emergencyOrder.getSalePrice();

        boolean menuChanged = currentMenu != null
                && nextMenu != null
                && !Objects.equals(currentMenu.getId(), nextMenu.getId());
        int nextStock = menuChanged
                ? safeToInt(valueOf(emergencyOrder.getQuantity()))
                : Math.addExact(safeState.stock(), safeToInt(valueOf(emergencyOrder.getQuantity())));

        return new InventoryState(nextMenu, nextStock, nextSalePrice);
    }

    private long valueOf(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private int safeToInt(long value) {
        return Math.toIntExact(Math.max(0L, value));
    }

    public record EmergencyOrderState(
            boolean pending,
            LocalDateTime arriveAt,
            Order latestArrivedOrder
    ) {
    }

    public record InventoryState(
            Menu menu,
            int stock,
            Integer salePrice
    ) {
    }
}
