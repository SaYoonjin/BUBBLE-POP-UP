package com.ssafy.S14P21A205.shop.repository;

import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.ItemUser;
import com.ssafy.S14P21A205.shop.entity.ItemUserId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemUserRepository extends JpaRepository<ItemUser, ItemUserId> {

    @Query("""
            SELECT iu
            FROM ItemUser iu
            JOIN FETCH iu.item i
            WHERE iu.user.id = :userId
              AND iu.isPurchased = true
            ORDER BY i.id ASC
            """)
    List<ItemUser> findPurchasedItemsByUserId(@Param("userId") Integer userId);

    List<ItemUser> findAllByUser_IdAndItem_IdIn(Integer userId, List<Long> itemIds);

    @Query("""
            SELECT CASE WHEN COUNT(iu) > 0 THEN true ELSE false END
            FROM ItemUser iu
            JOIN iu.item item
            WHERE iu.user.id = :userId
              AND iu.isPurchased = true
              AND item.category = :category
            """)
    boolean existsPurchasedCategoryItem(
            @Param("userId") Integer userId,
            @Param("category") ItemCategory category
    );

    @Query("""
            SELECT item.discountRate
            FROM ItemUser itemUser
            JOIN itemUser.item item
            WHERE itemUser.user.id = :userId
              AND itemUser.isPurchased = true
              AND item.category = :category
            """)
    Optional<BigDecimal> findPurchasedDiscountRateByUserIdAndCategory(
            @Param("userId") Integer userId,
            @Param("category") ItemCategory category
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ItemUser iu
            SET iu.isPurchased = false
            WHERE iu.user.id = :userId
              AND iu.isPurchased = true
            """)
    int resetPurchasedByUserId(@Param("userId") Integer userId);
}