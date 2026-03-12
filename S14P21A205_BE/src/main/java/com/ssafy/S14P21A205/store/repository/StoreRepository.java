package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.store.entity.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    Optional<Store> findFirstByUser_IdAndSeasonStatusOrderByIdDesc(Integer userId, SeasonStatus seasonStatus);

    Optional<Store> findByUser_Id(Integer userId);
}
