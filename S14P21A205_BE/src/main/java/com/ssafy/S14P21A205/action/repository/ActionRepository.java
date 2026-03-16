package com.ssafy.S14P21A205.action.repository;

import com.ssafy.S14P21A205.action.entity.Action;
import com.ssafy.S14P21A205.action.entity.ActionCategory;
import com.ssafy.S14P21A205.action.entity.PromotionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRepository extends JpaRepository<Action, Long> {

    List<Action> findByCategory(ActionCategory category);

    Optional<Action> findByCategoryAndPromotionType(ActionCategory category, PromotionType promotionType);
}
