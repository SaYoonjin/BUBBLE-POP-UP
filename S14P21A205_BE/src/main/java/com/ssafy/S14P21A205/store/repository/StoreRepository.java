package com.ssafy.S14P21A205.store.repository;

import com.ssafy.S14P21A205.store.entity.Store;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByUser_Id(UUID userId);

}