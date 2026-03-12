package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.Weather;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherRepository extends JpaRepository<Weather, Long> {

    List<Weather> findAllByOrderByIdAsc();
}
