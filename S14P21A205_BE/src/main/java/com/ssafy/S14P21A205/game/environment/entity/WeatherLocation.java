package com.ssafy.S14P21A205.game.environment.entity;

import com.ssafy.S14P21A205.store.entity.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "weather_location",
        uniqueConstraints = @UniqueConstraint(name = "uk_weather_location_day", columnNames = {"location_id", "day"})
)
@IdClass(WeatherLocationId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeatherLocation {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Id
    @Column(nullable = false)
    private Integer day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weather_id", nullable = false)
    private Weather weather;

    public static WeatherLocation create(Location location, Weather weather, Integer day) {
        WeatherLocation weatherLocation = new WeatherLocation();
        weatherLocation.location = location;
        weatherLocation.weather = weather;
        weatherLocation.day = day;
        return weatherLocation;
    }
}
