package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.LocationResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public StoreResponse getStore(Long userId) {
        Store store = getStoreByUserId(userId);

        return new StoreResponse(
                store.getLocation().getLocationName(),
                store.getStoreName(),
                store.getMenu().getMenuName(),
                store.getSeason().getCurrentDay()
        );
    }

    @Override
    @Transactional
    public UpdateStoreLocationResponse updateStoreLocation(Long userId, UpdateStoreLocationRequest request) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();

        BigDecimal rentDiscountRate = getRentDiscountRate(storeId);

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("지역을 찾을 수 없습니다."));

        if (store.getLocation().getId().equals(location.getId())) {
            throw new RuntimeException("이미 해당 지역에 매장이 위치해 있습니다.");
        }

        Integer discountedRent = applyDiscount(location.getRent(), rentDiscountRate);
        Integer updatedBalance = deductBalance(storeId, discountedRent);

        store.changeLocation(location);

        return new UpdateStoreLocationResponse(
                location.getId(),
                updatedBalance
        );
    }

    @Override
    public LocationListResponse getLocations(Long userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();

        BigDecimal rentDiscountRate = getRentDiscountRate(storeId);
        float discount = rentDiscountRate.floatValue();

        return new LocationListResponse(
                locationRepository.findAllByOrderByIdAsc().stream()
                        .map(location -> new LocationResponse(
                                location.getId(),
                                location.getLocationName(),
                                location.getRent(),
                                discount
                        ))
                        .toList()
        );
    }

    private Integer deductBalance(Long storeId, Integer amount) {
        String key = generateBalanceKey(storeId);
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

    private String generateBalanceKey(Long storeId) {
        return BALANCE_KEY_PREFIX + storeId;
    }

    private BigDecimal getRentDiscountRate(Long storeId) {
        return storeRepository
                .findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.RENT)
                .orElse(BigDecimal.ONE);
    }

    private Integer applyDiscount(Integer amount, BigDecimal discountRate) {
        BigDecimal normalizedDiscountRate = normalizeDiscountRate(discountRate);

        BigDecimal discountedAmount = BigDecimal.valueOf(amount)
                .multiply(normalizedDiscountRate)
                .setScale(0, RoundingMode.HALF_UP);

        return discountedAmount.intValue();
    }

    private BigDecimal normalizeDiscountRate(BigDecimal discountRate) {
        if (discountRate == null || discountRate.signum() <= 0) {
            return BigDecimal.ONE;
        }

        if (discountRate.compareTo(BigDecimal.ONE) > 0) {
            return discountRate.movePointLeft(2);
        }

        return discountRate;
    }

    private Store getStoreByUserId(Long userId) {
        return storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));
    }
}