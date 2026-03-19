package com.ssafy.S14P21A205.action.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "action")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", length = 20)
    private PromotionType promotionType;

    @Column(nullable = false)
    private Integer cost;

    @Column(name = "capture_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal captureRate;

    private Action(
            ActionCategory category,
            PromotionType promotionType,
            Integer cost,
            BigDecimal captureRate
    ) {
        this.category = category;
        this.promotionType = promotionType;
        this.cost = cost;
        this.captureRate = captureRate;
    }

    public static Action create(
            ActionCategory category,
            PromotionType promotionType,
            Integer cost,
            BigDecimal captureRate
    ) {
        return new Action(category, promotionType, cost, captureRate);
    }
}
