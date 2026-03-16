package com.ssafy.S14P21A205.game.season.repository;

import com.ssafy.S14P21A205.game.season.entity.SeasonRankingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRankingRecordRepository extends JpaRepository<SeasonRankingRecord, Long> {

    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu", "store.season"})
    List<SeasonRankingRecord> findByStore_Season_IdOrderByFinalRankAsc(Long seasonId);

    // 특정 시즌 + 특정 유저의 가게
    @EntityGraph(attributePaths = {"store", "store.user", "store.location", "store.menu", "store.season"})
    Optional<SeasonRankingRecord> findByStore_Season_IdAndStore_User_Id(Long seasonId, Integer userId);
}
