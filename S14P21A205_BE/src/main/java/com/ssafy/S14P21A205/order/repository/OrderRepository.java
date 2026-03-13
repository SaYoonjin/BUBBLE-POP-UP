package com.ssafy.S14P21A205.order.repository;

import com.ssafy.S14P21A205.order.entity.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByStoreIdAndOrderedDay(Long storeId, Integer orderedDay);
}
