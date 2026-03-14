package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.Season;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    // 특정 상태(IN_PROGRESS, FINISHED)의 가장 최근 시즌 1개 조회
    Optional<Season> findFirstByStatusOrderByIdDesc(SeasonStatus status);
}
