package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.store.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}