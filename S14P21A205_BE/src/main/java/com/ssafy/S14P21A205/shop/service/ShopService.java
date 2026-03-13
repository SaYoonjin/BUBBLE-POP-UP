package com.ssafy.S14P21A205.shop.service;

import com.ssafy.S14P21A205.exception.BaseException;
import com.ssafy.S14P21A205.exception.ErrorCode;
import com.ssafy.S14P21A205.shop.dto.PurchaseItemResponse;
import com.ssafy.S14P21A205.shop.dto.PurchaseItemsResponse;
import com.ssafy.S14P21A205.shop.dto.PurchasedItemListResponse;
import com.ssafy.S14P21A205.shop.dto.PurchasedItemResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemResponse;
import com.ssafy.S14P21A205.shop.entity.Item;
import com.ssafy.S14P21A205.shop.entity.ItemCategory;
import com.ssafy.S14P21A205.shop.entity.ItemUser;
import com.ssafy.S14P21A205.shop.repository.ItemRepository;
import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import com.ssafy.S14P21A205.store.entity.Store;
import com.ssafy.S14P21A205.store.repository.StoreRepository;
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ItemRepository itemRepository;
    private final ItemUserRepository itemUserRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    public ShopItemListResponse getShopItems() {
        List<Item> items = itemRepository.findAllByOrderByIdAsc();

        List<ShopItemResponse> itemResponses = items.stream()
                .map(ShopItemResponse::from)
                .toList();

        return ShopItemListResponse.of(itemResponses);
    }

    public PurchasedItemListResponse getPurchasedItems(Integer userId) {
        List<ItemUser> purchasedItems = itemUserRepository.findPurchasedItemsByUserId(userId);

        List<PurchasedItemResponse> responses = purchasedItems.stream()
                .map(itemUser -> PurchasedItemResponse.of(
                        itemUser.getItem().getId(),
                        itemUser.getItem().getDiscountRate()
                ))
                .toList();

        return PurchasedItemListResponse.of(responses);
    }

    @Transactional
    public PurchaseItemsResponse purchaseItems(Integer userId, List<Long> itemIds) {

        // 현재 유저, 매장, 아이템 조회 (아이템 안샀으면 바로 반환)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
        if (itemIds == null || itemIds.isEmpty()) {
            return PurchaseItemsResponse.of(
                    List.of(),
                    0,
                    user.getPoint()
            );
        }
        Store store = storeRepository.findFirstByUser_IdOrderBySeason_IdDescIdDesc(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.SHOP_STORE_NOT_FOUND));

        // 요청 아이템 조회 및 중복 카테고리 검사
        List<Item> items = getOrderedItems(itemIds);
        validateCategories(store.getId(), items);

        // 포인트 계산 및 부족 여부 판단
        int totalUsedPoints = items.stream()
                .mapToInt(Item::getPoint)
                .sum();

        if (user.getPoint() < totalUsedPoints) {
            throw new BaseException(ErrorCode.SHOP_INSUFFICIENT_POINTS);
        }

        // 포인트 차감 후 구매 기록 저장
        user.usePoints(totalUsedPoints);
        itemUserRepository.saveAll(items.stream()
                .map(item -> ItemUser.purchase(item, store))
                .toList());

        List<PurchaseItemResponse> purchasedItems = items.stream()
                .map(item -> PurchaseItemResponse.of(
                        item,
                        item.getDiscountRate()
                ))
                .toList();

        return PurchaseItemsResponse.of(
                purchasedItems,
                totalUsedPoints,
                user.getPoint()
        );
    }

    private List<Item> getOrderedItems(List<Long> itemIds) {
        Map<Long, Item> itemMap = itemRepository.findAllById(itemIds).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        if (itemMap.size() != itemIds.size()) {
            throw new BaseException(ErrorCode.SHOP_ITEM_NOT_FOUND);
        }

        return itemIds.stream()
                .map(itemMap::get)
                .toList();
    }

    // 같은 카테고리 중복 구매 방지
    private void validateCategories(Long storeId, List<Item> items) {
        Set<ItemCategory> requestCategories = new LinkedHashSet<>();

        for (Item item : items) {
            if (!requestCategories.add(item.getCategory())) {
                throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "같은 카테고리의 아이템은 함께 구매할 수 없습니다.");
            }

            if (itemUserRepository.existsPurchasedCategoryItem(storeId, item.getCategory())) {
                throw new BaseException(ErrorCode.SHOP_ITEM_ALREADY_PURCHASED, "이미 해당 카테고리의 아이템을 보유하고 있습니다.");
            }
        }
    }
}
