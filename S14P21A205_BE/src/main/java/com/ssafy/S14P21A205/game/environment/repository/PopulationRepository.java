package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.Population;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PopulationRepository extends JpaRepository<Population, Long> {

    List<Population> findByLocationIdOrderByDateAsc(Long locationId);

    @Query("""
            SELECT p.location.locationName, AVG(p.floatingPopulation)
            FROM Population p
            GROUP BY p.location.locationName
            ORDER BY AVG(p.floatingPopulation) DESC
            """)
    List<Object[]> avgPopulationByLocation();
}
