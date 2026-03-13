package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.order.dto.CurrentOrderResponse;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Store;
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
public class OrderServiceImpl implements OrderService {

    private static final String STOCK_KEY_PREFIX = "stock:";

    private final StoreRepository storeRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CurrentOrderResponse getCurrentOrder(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        Menu menu = store.getMenu();

        BigDecimal ingredientDiscountRate = getIngredientDiscountRate(storeId);
        Integer discountedCostPrice = applyDiscount(menu.getOriginPrice(), ingredientDiscountRate);
        Integer stock = getStock(storeId);

        return CurrentOrderResponse.builder()
                .menuId(Math.toIntExact(menu.getId()))
                .menuName(menu.getMenuName())
                .costPrice(discountedCostPrice)
                .sellingPrice(store.getPrice())
                .stock(stock)
                .build();
    }

    private Store getStoreByUserId(Integer userId) {
        return storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));
    }

    private BigDecimal getIngredientDiscountRate(Long storeId) {
        return normalizeDiscountRate(
                storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.INGREDIENT)
                        .orElse(BigDecimal.ZERO)
        );
    }

    private Integer applyDiscount(Integer originalPrice, BigDecimal discountRate) {
        if (discountRate == null || discountRate.signum() <= 0) {
            return originalPrice;
        }

        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(discountRate);

        return BigDecimal.valueOf(originalPrice)
                .multiply(discountMultiplier)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
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

    private Integer getStock(Long storeId) {
        String key = generateStockKey(storeId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        return Integer.valueOf(value);
    }

    private String generateStockKey(Long storeId) {
        return STOCK_KEY_PREFIX + storeId;
    }
}