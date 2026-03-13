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

    // Redis에 저장된 재고 조회 시 사용하는 key prefix
    private static final String STOCK_KEY_PREFIX = "stock:";

    private final StoreRepository storeRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 현재 판매가 조회
     * 1. userId로 매장 조회
     * 2. 매장에서 판매 중인 메뉴 조회
     * 3. 원재료 할인 아이템 구매 여부 확인
     * 4. 할인율을 적용한 원가 계산
     * 5. Redis에서 현재 재고 조회
     */
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
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.INGREDIENT)
                .orElse(BigDecimal.ONE);
    }

    private Integer applyDiscount(Integer originalPrice, BigDecimal discountRate) {
        if (discountRate == null || discountRate.signum() <= 0) {
            return originalPrice;
        }

        return BigDecimal.valueOf(originalPrice)
                .multiply(discountRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    // Redis에서 현재 매장 재고 조회
    private Integer getStock(Long storeId) {
        String key = generateStockKey(storeId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        return Integer.valueOf(value);
    }

    /**
     * Redis 재고 조회 key 생성
     * 형식: stock:{storeId}
     */
    private String generateStockKey(Long storeId) {
        return STOCK_KEY_PREFIX + storeId;
    }
}
