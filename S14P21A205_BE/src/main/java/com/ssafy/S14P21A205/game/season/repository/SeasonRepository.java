package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    Optional<Season> findFirstByStatusOrderByIdDesc(SeasonStatus status);

    Optional<Season> findFirstByStatusOrderByStartTimeAscIdAsc(SeasonStatus status);

    Optional<Season> findFirstBySourceBatchKeyIsNotNullOrderByIdDesc();

    Optional<Season> findFirstByOrderByIdDesc();

    Optional<Season> findByIdAndStatus(Long id, SeasonStatus status);

    boolean existsByStatusAndStartTime(SeasonStatus status, LocalDateTime startTime);
}
