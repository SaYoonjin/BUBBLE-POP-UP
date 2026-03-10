package com.ssafy.S14P21A205.game.environment.entity;

import com.ssafy.S14P21A205.shop.entity.Menu;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "weather_menu")
@IdClass(WeatherMenuId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherMenu {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weather_id", nullable = false)
    private Weather weather;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "menu_sales_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal menuSalesRate;
}
