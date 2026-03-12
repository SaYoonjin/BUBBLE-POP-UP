package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private static final String BALANCE_KEY_PREFIX = "balance:";

    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public StoreResponse getStore(UUID userId) {
        Store store = storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));

        return new StoreResponse(
                store.getLocation().getLocationName(),
                store.getStoreName(),
                store.getMenu().getMenuName(),
                store.getSeason().getCurrentDay()
        );
    }

    @Override
    @Transactional
    public UpdateStoreLocationResponse updateStoreLocation(UUID userId, UpdateStoreLocationRequest request) {
        Store store = storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("지역을 찾을 수 없습니다."));

        if (store.getLocation().getId().equals(location.getId())) {
            throw new RuntimeException("이미 현재 지역으로 설정되어 있습니다.");
        }

        Integer updatedBalance = deductBalance(userId, location.getRent());

        store.changeLocation(location);

        return new UpdateStoreLocationResponse(
                location.getId(),
                updatedBalance
        );
    }

    private Integer deductBalance(UUID userId, Integer amount) {
        String key = generateBalanceKey(userId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new RuntimeException("잔액 정보를 찾을 수 없습니다.");
        }

        Integer currentBalance = Integer.valueOf(value);

        if (currentBalance < amount) {
            throw new RuntimeException("잔액이 부족합니다.");
        }

        Integer updatedBalance = currentBalance - amount;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(updatedBalance));

        return updatedBalance;
    }

    private String generateBalanceKey(UUID userId) {
        return BALANCE_KEY_PREFIX + userId;
    }
}