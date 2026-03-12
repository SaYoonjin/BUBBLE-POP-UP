package com.ssafy.S14P21A205.shop.repository;

import com.ssafy.S14P21A205.shop.entity.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByIdAsc();
}