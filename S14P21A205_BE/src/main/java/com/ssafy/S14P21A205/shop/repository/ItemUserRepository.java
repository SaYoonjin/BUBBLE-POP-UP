package com.ssafy.S14P21A205.shop.repository;

import com.ssafy.S14P21A205.shop.entity.ItemUser;
import com.ssafy.S14P21A205.shop.entity.ItemUserId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemUserRepository extends JpaRepository<ItemUser, ItemUserId> {

    /**
     * 로그인한 사용자의 최신 시즌 매장에서 구매한 아이템 목록 조회
     */
    @Query("""
            SELECT iu
            FROM ItemUser iu
            JOIN FETCH iu.item i
            JOIN FETCH iu.store s
            WHERE s.user.id = :userId
              AND iu.isPurchased = true
              AND s.season.id = (
                    SELECT MAX(s2.season.id)
                    FROM Store s2
                    WHERE s2.user.id = :userId
              )
            ORDER BY i.id ASC
            """)
    List<ItemUser> findPurchasedItemsByUserId(@Param("userId") Long userId);
}