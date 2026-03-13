package com.ssafy.S14P21A205.shop.repository;

import com.ssafy.S14P21A205.shop.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}