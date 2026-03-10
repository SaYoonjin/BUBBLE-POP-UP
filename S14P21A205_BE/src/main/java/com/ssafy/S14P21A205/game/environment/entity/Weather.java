package com.ssafy.S14P21A205.game.environment.entity;

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
@Table(name = "weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weather_id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_type", nullable = false, length = 20)
    private WeatherType weatherType;

    @Column(name = "population_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal populationPercent;
}
