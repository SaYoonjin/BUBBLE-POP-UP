package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.store.entity.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    Optional<Store> findFirstByUser_IdAndSeasonStatusOrderByIdDesc(Integer userId, SeasonStatus seasonStatus);

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    @Query("""
            select s
            from Store s
            where s.user.id = :userId
              and s.season.status = :seasonStatus
              and not exists (
                    select 1
                    from SeasonRankingRecord record
                    where record.store = s
              )
            order by s.id desc
            """)
    List<Store> findActiveStoresByUserIdAndSeasonStatusOrderByIdDesc(
            @Param("userId") Integer userId,
            @Param("seasonStatus") SeasonStatus seasonStatus
    );

    Optional<Store> findFirstByUser_IdOrderBySeason_IdDescIdDesc(Integer userId);

    Optional<Store> findFirstByUser_IdAndSeason_IdOrderByIdDesc(Integer userId, Long seasonId);

    boolean existsByUser_IdAndSeason_Id(Integer userId, Long seasonId);

    Optional<Store> findByUser_Id(Integer userId);

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    List<Store> findBySeason_IdOrderByIdAsc(Long seasonId);

    long countBySeason_IdAndLocation_Id(Long seasonId, Long locationId);

    Optional<Store> findByUserId(Integer userId);

    @Query("""
            select coalesce(avg(s.price), 0)
            from Store s
            where s.season.id = :seasonId
              and s.menu.id = :menuId
            """)
    int findAveragePriceBySeasonIdAndMenuId(
            @Param("seasonId") Long seasonId,
            @Param("menuId") Long menuId
    );

    default Optional<Store> findFirstActiveByUser_IdAndSeasonStatusOrderByIdDesc(Integer userId, SeasonStatus seasonStatus) {
        return findActiveStoresByUserIdAndSeasonStatusOrderByIdDesc(userId, seasonStatus).stream().findFirst();
    }
}
