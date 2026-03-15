package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    Optional<Store> findFirstByUser_IdAndSeasonStatusOrderByIdDesc(Integer userId, SeasonStatus seasonStatus);

    Optional<Store> findFirstByUser_IdOrderBySeason_IdDescIdDesc(Integer userId);

    Optional<Store> findByUser_Id(Integer userId);

    @EntityGraph(attributePaths = {"user", "location", "menu", "season"})
    List<Store> findBySeason_IdOrderByIdAsc(Long seasonId);

    @Query("""
            select item.discountRate
            from ItemUser itemUser
            join itemUser.item item
            where itemUser.store.id = :storeId
              and itemUser.isPurchased = true
              and item.category = :category
            """)
    Optional<BigDecimal> findPurchasedDiscountRateByStoreIdAndCategory(
            @Param("storeId") Long storeId,
            @Param("category") ItemCategory category
    );

    Optional<Store> findByUserId(Integer userId);
}
