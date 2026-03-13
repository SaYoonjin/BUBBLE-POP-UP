package com.ssafy.S14P21A205.order.service;

import com.ssafy.S14P21A205.order.dto.CurrentOrderResponse;
import com.ssafy.S14P21A205.order.dto.RegularOrderRequest;
import com.ssafy.S14P21A205.order.dto.RegularOrderResponse;
import com.ssafy.S14P21A205.order.entity.Order;
import com.ssafy.S14P21A205.order.repository.OrderRepository;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final String STOCK_KEY_PREFIX = "stock:";
    private static final String BALANCE_KEY_PREFIX = "balance:";
    private static final Set<Integer> REGULAR_ORDER_DAYS = Set.of(0, 2, 4, 6);

    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final EntityManager entityManager;

    /**
     * 현재 매장에서 판매 중인 메뉴의 가격 및 재고 조회
     * 1. userId로 매장 조회
     * 2. 매장에서 판매 중인 메뉴 조회
     * 3. 원재료 할인 아이템 적용 여부 확인
     * 4. 할인 적용 원가 계산
     * 5. Redis에서 재고 조회
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

    /**
     * 정규 발주 생성
     * 처리 흐름
     * 1. userId로 매장 조회
     * 2. 현재 시즌 기준 발주 가능 일자 계산
     * 3. 정규 발주 가능 일자 검증
     * 4. 발주 수량 검증
     * 5. 동일 발주일 중복 발주 여부 확인
     * 6. 발주할 메뉴 조회
     * 7. 현재 판매 메뉴와 동일한지 확인
     * 8. 할인 적용 원가 계산
     * 9. 총 발주 비용 계산
     * 10. Redis 잔액 차감
     * 11. Redis 재고 업데이트
     * 12. 필요 시 매장 판매 메뉴 변경
     * 13. orders 테이블에 발주 기록 저장
     */
    @Override
    @Transactional
    public RegularOrderResponse createRegularOrder(Integer userId, RegularOrderRequest request) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        int regularOrderDay = resolveRegularOrderDay(store.getSeason().getCurrentDay());
        int orderedDay = resolveOrderedDay(regularOrderDay);

        validateRegularOrderDay(regularOrderDay);
        validateQuantity(request.quantity());
        validateNoExistingOrder(storeId, orderedDay);

        Menu menu = getMenuById(request.menuId());
        boolean sameMenu = Objects.equals(store.getMenu().getId(), menu.getId());

        BigDecimal discountRate = getIngredientDiscountRate(storeId);
        Integer costPrice = resolveCostPrice(menu, discountRate);
        Integer totalCost = Math.multiplyExact(costPrice, request.quantity());

        deductBalance(storeId, totalCost);
        updateStock(storeId, request.quantity(), sameMenu);
        if (!sameMenu) {
            updateStoreMenu(storeId, menu.getId());
        }

        Order savedOrder = orderRepository.save(
                Order.create(menu, store, request.quantity(), totalCost, orderedDay)
        );

        return RegularOrderResponse.builder()
                .orderId(savedOrder.getId())
                .menuId(Math.toIntExact(menu.getId()))
                .quantity(request.quantity())
                .costPrice(costPrice)
                .totalCost(totalCost)
                .discount(discountRate.floatValue())
                .build();
    }

    private Store getStoreByUserId(Integer userId) {
        return storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));
    }

    private Menu getMenuById(Integer menuId) {
        return menuRepository.findById(Long.valueOf(menuId))
                .orElseThrow(() -> new RuntimeException("메뉴를 찾을 수 없습니다."));
    }

    private BigDecimal getIngredientDiscountRate(Long storeId) {
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.INGREDIENT)
                .orElse(BigDecimal.ONE);
    }

    private Integer resolveCostPrice(Menu menu, BigDecimal discountRate) {
        return applyDiscount(menu.getOriginPrice(), discountRate);
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

    private void validateRegularOrderDay(int currentDay) {
        if (!REGULAR_ORDER_DAYS.contains(currentDay)) {
            throw new RuntimeException("정규 발주는 0, 2, 4, 6일차에만 가능합니다.");
        }
    }

    private int resolveRegularOrderDay(Integer currentDay) {
        return currentDay == null ? 0 : currentDay;
    }

    private int resolveOrderedDay(int regularOrderDay) {
        return regularOrderDay == 0 ? 1 : regularOrderDay;
    }

    private void validateNoExistingOrder(Long storeId, Integer orderedDay) {
        if (orderRepository.findDailyStartOrder(storeId, orderedDay).isPresent()) {
            throw new RuntimeException("해당 발주일에는 이미 정규 발주가 완료되었습니다.");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 50 || quantity > 500) {
            throw new RuntimeException("발주 수량은 50~500 사이여야 합니다.");
        }
    }

    private Integer getStock(Long storeId) {
        String key = generateStockKey(storeId);
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return 0;
        }

        return Integer.valueOf(value);
    }

    private void updateStock(Long storeId, Integer quantity, boolean sameMenu) {
        String key = generateStockKey(storeId);
        int updatedStock = sameMenu ? getStock(storeId) + quantity : quantity;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(updatedStock));
    }

    private void updateStoreMenu(Long storeId, Long menuId) {
        entityManager.createNativeQuery("UPDATE store SET menu_id = :menuId WHERE store_id = :storeId")
                .setParameter("menuId", menuId)
                .setParameter("storeId", storeId)
                .executeUpdate();
    }

    private void deductBalance(Long storeId, Integer amount) {
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
    }

    private String generateStockKey(Long storeId) {
        return STOCK_KEY_PREFIX + storeId;
    }

    private String generateBalanceKey(Long storeId) {
        return BALANCE_KEY_PREFIX + storeId;
    }
}
