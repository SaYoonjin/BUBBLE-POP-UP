package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.WeatherLocation;
import com.ssafy.S14P21A205.game.environment.entity.WeatherLocationId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherLocationRepository extends JpaRepository<WeatherLocation, WeatherLocationId> {

    Optional<WeatherLocation> findByLocation_IdAndDay(Long locationId, Integer day);

    List<WeatherLocation> findByDayOrderByLocation_IdAsc(Integer day);
}
