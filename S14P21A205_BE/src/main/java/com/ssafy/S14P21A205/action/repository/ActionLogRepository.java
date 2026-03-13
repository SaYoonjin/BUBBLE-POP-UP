package com.ssafy.S14P21A205.action.repository;

import com.ssafy.S14P21A205.action.entity.ActionLog;
import com.ssafy.S14P21A205.action.entity.ActionLogId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionLogRepository extends JpaRepository<ActionLog, ActionLogId> {

    @EntityGraph(attributePaths = {"action"})
    List<ActionLog> findByStore_IdAndGameDayAndIsUsedTrue(Long storeId, Integer gameDay);
}
