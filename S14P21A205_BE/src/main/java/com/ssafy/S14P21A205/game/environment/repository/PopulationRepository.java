package com.ssafy.S14P21A205.game.environment.repository;

import com.ssafy.S14P21A205.game.environment.entity.Population;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopulationRepository extends JpaRepository<Population, Long> {

    List<Population> findByLocationIdOrderByDateAsc(Long locationId);

    @Query("""
            SELECT p.location.locationName, AVG(p.floatingPopulation)
            FROM Population p
            GROUP BY p.location.locationName
            ORDER BY AVG(p.floatingPopulation) DESC
            """)
    List<Object[]> avgPopulationByLocation();

    @Query("""
            SELECT DISTINCT CAST(p.date AS LocalDate)
            FROM Population p
            ORDER BY CAST(p.date AS LocalDate)
            """)
    List<LocalDate> findDistinctDatesOrdered();

    @Query("""
            SELECT p.location.locationName, AVG(p.floatingPopulation)
            FROM Population p
            WHERE CAST(p.date AS LocalDate) = :targetDate
            GROUP BY p.location.locationName
            ORDER BY AVG(p.floatingPopulation) DESC
            """)
    List<Object[]> avgPopulationByLocationAndDate(@Param("targetDate") LocalDate targetDate);
}
