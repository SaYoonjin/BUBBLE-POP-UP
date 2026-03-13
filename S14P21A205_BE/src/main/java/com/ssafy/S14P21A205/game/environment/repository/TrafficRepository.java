package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.Traffic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficRepository extends JpaRepository<Traffic, Long> {

    List<Traffic> findByLocationIdOrderByDateAsc(Long locationId);
}
