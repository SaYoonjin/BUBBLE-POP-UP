package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.game.day.state.repository.GameDayStoreStateRedisRepository;
import com.ssafy.S14P21A205.game.season.entity.SeasonStatus;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final LocationRepository locationRepository;
    private final MenuRepository menuRepository;
    private final ItemUserRepository itemUserRepository;
    private final GameDayStoreStateRedisRepository gameDayStoreStateRedisRepository;

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

    @Override
    @Transactional
    public UpdateStoreLocationResponse updateStoreLocation(Integer userId, UpdateStoreLocationRequest request) {
        Store store = getStoreByUserId(userId);
        Long storeId = store.getId();
        int currentDay = resolveCurrentDay(store.getSeason().getCurrentDay());

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new RuntimeException("Location was not found."));

        if (store.getLocation().getId().equals(location.getId())) {
            throw new RuntimeException("The store is already using this location.");
        }

        Integer updatedBalance = deductBalance(storeId, currentDay, location.getInteriorCost());
        store.changeLocation(location);

        return new UpdateStoreLocationResponse(
                location.getId(),
                updatedBalance
        );
    }

    @Override
    public LocationListResponse getLocations(Integer userId) {
        getStoreByUserId(userId);
        float discount = getDisplayedRentDiscountRate(userId).floatValue();

        return new LocationListResponse(
                locationRepository.findAllByOrderByIdAsc().stream()
                        .map(location -> new LocationResponse(
                                location.getId(),
                                location.getLocationName(),
                                location.getRent(),
                                location.getInteriorCost(),
                                discount
                        ))
                        .toList()
        );
    }

    @Override
    public MenuListResponse getMenus(Integer userId) {
        getStoreByUserId(userId);
        float discount = getDisplayedIngredientDiscountRate(userId).floatValue();

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

    private Integer deductBalance(Long storeId, int day, Integer amount) {
        long currentBalance = gameDayStoreStateRedisRepository.findBalance(storeId, day)
                .orElseThrow(() -> new RuntimeException("Balance information was not found."));

        if (currentBalance < amount) {
            throw new RuntimeException("Balance is insufficient.");
        }

        long updatedBalance = currentBalance - amount;
        gameDayStoreStateRedisRepository.saveBalance(storeId, day, updatedBalance);
        return Math.toIntExact(updatedBalance);
    }

    private BigDecimal getDisplayedRentDiscountRate(Integer userId) {
        return itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(userId, ItemCategory.RENT)
                .orElse(BigDecimal.ONE);
    }

    private BigDecimal getDisplayedIngredientDiscountRate(Integer userId) {
        return itemUserRepository.findPurchasedDiscountRateByUserIdAndCategory(userId, ItemCategory.INGREDIENT)
                .orElse(BigDecimal.ONE);
    }

    private int resolveCurrentDay(Integer currentDay) {
        return currentDay == null || currentDay == 0 ? 1 : currentDay;
    }

    private Store getStoreByUserId(Integer userId) {
        return storeRepository.findFirstByUser_IdAndSeasonStatusOrderByIdDesc(userId, SeasonStatus.IN_PROGRESS)
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));
    }
}
