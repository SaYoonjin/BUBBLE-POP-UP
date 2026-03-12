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
    public StoreResponse getStore(Long storeId) {
        Store store = getStoreById(storeId);

        return new StoreResponse(
                store.getLocation().getLocationName(),
                store.getStoreName(),
                store.getMenu().getMenuName(),
                store.getSeason().getCurrentDay()
        );
    }

    @Override
    @Transactional
    public UpdateStoreLocationResponse updateStoreLocation(Long storeId, UpdateStoreLocationRequest request) {
        Store store = getStoreById(storeId);
        BigDecimal rentDiscountRate = getRentDiscountRate(storeId);

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("Location not found."));

        if (store.getLocation().getId().equals(location.getId())) {
            throw new RuntimeException("Store is already assigned to this location.");
        }

        Integer discountedRent = applyDiscount(location.getRent(), rentDiscountRate);
        Integer updatedBalance = deductBalance(store.getUser().getId(), discountedRent);

        store.changeLocation(location);

        return new UpdateStoreLocationResponse(
                location.getId(),
                updatedBalance
        );
    }

    @Override
    public LocationListResponse getLocations(Long storeId) {
        getStoreById(storeId);
        BigDecimal rentDiscountRate = getRentDiscountRate(storeId);
        float discount = rentDiscountRate.floatValue();

        return new LocationListResponse(
                locationRepository.findAll().stream()
                        .map(location -> new LocationResponse(
                                location.getId(),
                                location.getLocationName(),
                                location.getRent(),
                                discount
                        ))
                        .toList()
        );
    }

    private Integer deductBalance(UUID userId, Integer amount) {
        String key = generateBalanceKey(userId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            throw new RuntimeException("Balance information not found.");
        }

        Integer currentBalance = Integer.valueOf(value);

        if (currentBalance < amount) {
            throw new RuntimeException("Insufficient balance.");
        }

        Integer updatedBalance = currentBalance - amount;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(updatedBalance));

        return updatedBalance;
    }

    private String generateBalanceKey(UUID userId) {
        return BALANCE_KEY_PREFIX + userId;
    }

    private BigDecimal getRentDiscountRate(Long storeId) {
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.RENT)
                .orElse(BigDecimal.ZERO);
    }

    private Integer applyDiscount(Integer amount, BigDecimal discountRate) {
        BigDecimal normalizedDiscountRate = normalizeDiscountRate(discountRate);
        BigDecimal discountedAmount = BigDecimal.valueOf(amount)
                .multiply(BigDecimal.ONE.subtract(normalizedDiscountRate))
                .setScale(0, RoundingMode.HALF_UP);

        return discountedAmount.intValue();
    }

    private BigDecimal normalizeDiscountRate(BigDecimal discountRate) {
        if (discountRate == null || discountRate.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (discountRate.compareTo(BigDecimal.ONE) > 0) {
            return discountRate.movePointLeft(2);
        }
        return discountRate;
    }

    private Store getStoreById(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found."));
    }
}
