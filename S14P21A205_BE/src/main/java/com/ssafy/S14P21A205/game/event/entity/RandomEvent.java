package com.ssafy.S14P21A205.game.event.entity;

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
@Table(name = "random_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RandomEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", nullable = false, length = 50)
    private EventCategory eventCategory;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_time", nullable = false, length = 20)
    private EventStartTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_time", nullable = false, length = 20)
    private EventEndTime endTime;

    @Column(name = "population_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal populationRate;

    @Column(name = "stock_flat", nullable = false, precision = 5, scale = 2)
    private BigDecimal stockFlat;

    @Column(name = "cost_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal costRate;

    @Column(name = "capital_flat", nullable = false)
    private Integer capitalFlat;

    public static RandomEvent create(
            EventCategory eventCategory,
            String eventName,
            EventStartTime startTime,
            EventEndTime endTime,
            BigDecimal populationRate,
            BigDecimal stockFlat,
            BigDecimal costRate,
            Integer capitalFlat
    ) {
        RandomEvent randomEvent = new RandomEvent();
        randomEvent.sync(
                eventCategory,
                eventName,
                startTime,
                endTime,
                populationRate,
                stockFlat,
                costRate,
                capitalFlat
        );
        return randomEvent;
    }

    public void sync(
            EventCategory eventCategory,
            String eventName,
            EventStartTime startTime,
            EventEndTime endTime,
            BigDecimal populationRate,
            BigDecimal stockFlat,
            BigDecimal costRate,
            Integer capitalFlat
    ) {
        this.eventCategory = eventCategory;
        this.eventName = eventName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.populationRate = populationRate;
        this.stockFlat = stockFlat;
        this.costRate = costRate;
        this.capitalFlat = capitalFlat;
    }
}
