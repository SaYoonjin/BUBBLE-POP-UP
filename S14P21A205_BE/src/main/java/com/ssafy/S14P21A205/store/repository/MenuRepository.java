package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.shop.entity.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByOrderByIdAsc();

    Optional<Menu> findFirstByOrderByIdAsc();
}
