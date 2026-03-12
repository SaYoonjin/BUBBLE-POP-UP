package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.Population;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopulationRepository extends JpaRepository<Population, Long> {

    List<Population> findByLocationIdOrderByDateAsc(Long locationId);
}
