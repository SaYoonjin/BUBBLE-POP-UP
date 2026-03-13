package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.Menu;
import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.LocationResponse;
import com.ssafy.S14P21A205.store.dto.MenuListResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;
import com.ssafy.S14P21A205.store.entity.Location;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.LocationRepository;
import com.ssafy.S14P21A205.store.repository.MenuRepository;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    private final MenuRepository menuRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public StoreResponse getStore(Integer userId) {
        Store store = getStoreByUserId(userId);

        return new StoreResponse(
                store.getLocation().getLocationName(),
                store.getStoreName(),
                store.getMenu().getMenuName(),
                store.getSeason().getCurrentDay()
        );
    }

    /**
     * 매장 위치 변경
     * 1. userId로 매장 조회
     * 2. RENT 할인 아이템 보유 여부 확인
     * 3. 변경할 지역 조회
     * 4. 현재 지역과 동일한지 검증
     * 5. 할인 적용 임대료 계산
     * 6. Redis 잔액 차감
     * 7. 매장 위치 변경
     */
    @Override
    @Transactional
    public UpdateStoreLocationResponse updateStoreLocation(Integer userId, UpdateStoreLocationRequest request) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();

        BigDecimal rentDiscountMultiplier = getRentDiscountMultiplier(storeId);

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("지역을 찾을 수 없습니다."));

        if (store.getLocation().getId().equals(location.getId())) {
            throw new RuntimeException("이미 해당 지역에 매장이 위치해 있습니다.");
        }

        Integer discountedRent = applyDiscount(location.getRent(), rentDiscountMultiplier);
        Integer updatedBalance = deductBalance(storeId, discountedRent);

        store.changeLocation(location);

        return new UpdateStoreLocationResponse(
                location.getId(),
                updatedBalance
        );
    }

    /**
     * 지역 목록 조회
     * - 전체 지역 목록 반환
     * - RENT 할인 아이템이 있으면 할인율도 함께 표시
     */
    @Override
    public LocationListResponse getLocations(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        float discount = getDisplayedRentDiscountRate(storeId).floatValue();

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

    /**
     * 메뉴 목록 조회
     * - 전체 메뉴 목록 반환
     * - INGREDIENT 할인 아이템이 있으면 할인율도 함께 표시
     */
    @Override
    public MenuListResponse getMenus(Integer userId) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        float discount = getDisplayedIngredientDiscountRate(storeId).floatValue();

        List<MenuListResponse.MenuInfo> menuInfos = menuRepository.findAllByOrderByIdAsc().stream()
                .map(menu -> MenuListResponse.MenuInfo.builder()
                        .menuId(Math.toIntExact(menu.getId()))
                        .menuName(menu.getMenuName())
                        .ingredientPrice(menu.getOriginPrice())
                        .discount(discount)
                        .build())
                .toList();

        return MenuListResponse.builder()
                .menus(menuInfos)
                .build();
    }

    /**
     * Redis에서 매장 잔액 차감
     * - 잔액 정보가 없으면 예외
     * - 잔액 부족 시 예외
     * - 차감 후 Redis에 반영
     */
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

    private BigDecimal getRentDiscountMultiplier(Long storeId) {
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.RENT)
                .orElse(BigDecimal.ONE);
    }

    private BigDecimal getDisplayedRentDiscountRate(Long storeId) {
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.RENT)
                .orElse(BigDecimal.ONE);
    }

    private BigDecimal getDisplayedIngredientDiscountRate(Long storeId) {
        return storeRepository.findPurchasedDiscountRateByStoreIdAndCategory(storeId, ItemCategory.INGREDIENT)
                .orElse(BigDecimal.ONE);
    }

    private Integer applyDiscount(Integer amount, BigDecimal discountMultiplier) {
        BigDecimal discountedAmount = BigDecimal.valueOf(amount)
                .multiply(discountMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        return discountedAmount.intValue();
    }

    private Store getStoreByUserId(Integer userId) {
        return storeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("매장을 찾을 수 없습니다."));
    }
}
