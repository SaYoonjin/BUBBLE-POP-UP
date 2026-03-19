package com.ssafy.S14P21A205.order.entity;

import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_cost", nullable = false)
    private Integer totalCost;

    @Column(name = "sale_price")
    private Integer salePrice;

    @Column(name = "ordered_day", nullable = false)
    private Integer orderedDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;

    @Column(name = "arrived_time")
    private LocalDateTime arrivedTime;

    @Column(name = "is_arrived")
    private Boolean isArrived;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Order(
            Menu menu,
            Store store,
            Integer quantity,
            Integer totalCost,
            Integer salePrice,
            Integer orderedDay,
            OrderType orderType,
            LocalDateTime arrivedTime,
            Boolean isArrived
    ) {
        this.menu = menu;
        this.store = store;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.salePrice = salePrice;
        this.orderedDay = orderedDay;
        this.orderType = orderType;
        this.arrivedTime = arrivedTime;
        this.isArrived = isArrived;
    }

    public static Order create(Menu menu, Store store, Integer quantity, Integer totalCost, Integer orderedDay) {
        return create(menu, store, quantity, totalCost, null, orderedDay);
    }

    public static Order create(
            Menu menu,
            Store store,
            Integer quantity,
            Integer totalCost,
            Integer salePrice,
            Integer orderedDay
    ) {
        return new Order(menu, store, quantity, totalCost, salePrice, orderedDay, OrderType.NORMAL, null, true);
    }

    public static Order createEmergency(Menu menu, Store store, Integer quantity, Integer totalCost,
                                         Integer orderedDay, LocalDateTime arrivedTime) {
        return createEmergency(menu, store, quantity, totalCost, null, orderedDay, arrivedTime);
    }

    public static Order createEmergency(
            Menu menu,
            Store store,
            Integer quantity,
            Integer totalCost,
            Integer salePrice,
            Integer orderedDay,
            LocalDateTime arrivedTime
    ) {
        return new Order(menu, store, quantity, totalCost, salePrice, orderedDay, OrderType.EMERGENCY, arrivedTime, false);
    }

    public void markArrived() {
        this.isArrived = true;
    }
}
