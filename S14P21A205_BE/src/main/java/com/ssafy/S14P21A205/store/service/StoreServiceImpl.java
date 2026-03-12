package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    @Override
    public StoreResponse getStore(Integer userId) {
        Store store = storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));

        return new StoreResponse(
                store.getLocation().getLocationName(),
                store.getStoreName(),
                store.getMenu().getMenuName(),
                store.getSeason().getCurrentDay()
        );
    }
}