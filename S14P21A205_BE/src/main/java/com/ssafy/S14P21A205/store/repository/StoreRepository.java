package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.store.entity.Store;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByUser_Id(UUID userId);

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
}
