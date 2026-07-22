package com.ssafy.S14P21A205.game.day.repository;

import com.ssafy.S14P21A205.game.day.entity.DayClosingJob;
import com.ssafy.S14P21A205.game.day.entity.DayClosingJobStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DayClosingJobRepository extends JpaRepository<DayClosingJob, Long> {

    boolean existsBySeason_IdAndDay(Long seasonId, Integer day);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT job
            FROM DayClosingJob job
            WHERE job.season.id = :seasonId AND job.day = :day
            """)
    Optional<DayClosingJob> findBySeasonIdAndDayForUpdate(
            @Param("seasonId") Long seasonId,
            @Param("day") Integer day
    );

    @EntityGraph(attributePaths = "season")
    List<DayClosingJob> findTop50ByStatusNotAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            DayClosingJobStatus status,
            LocalDateTime now
    );
}
