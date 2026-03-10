package com.ssafy.S14P21A205.game.season.entity;

import com.ssafy.S14P21A205.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "daily_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer day;

    @Column(nullable = false)
    private Integer revenue;

    @Column(name = "total_cost", nullable = false)
    private Integer totalCost;

    @Column(name = "net_profit", nullable = false)
    private Integer netProfit;

    @Column(nullable = false)
    private Integer visitors;

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount;

    @Column(name = "stock_remaining", nullable = false)
    private Integer stockRemaining;

    @Column(name = "consecutive_deficit_days", nullable = false)
    private Integer consecutiveDeficitDays;

    @Column(name = "is_bankrupt", nullable = false)
    private Boolean isBankrupt;

    @Column(nullable = false)
    private Integer balance;

    @Column(name = "capture_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal captureRate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
