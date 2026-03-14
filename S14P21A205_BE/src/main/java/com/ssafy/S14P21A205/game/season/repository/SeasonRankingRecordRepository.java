package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.SeasonRankingRecord;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRankingRecordRepository extends JpaRepository<SeasonRankingRecord, Long> {

    // 확정된 최종 랭킹 레코드 조회(오름차순)
    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu", "store.season"})
    List<SeasonRankingRecord> findByStore_Season_IdOrderByFinalRankAsc(Long seasonId);
}
