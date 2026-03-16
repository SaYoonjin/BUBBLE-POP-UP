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
import com.ssafy.S14P21A205.user.entity.User;
import com.ssafy.S14P21A205.user.repository.UserRepository;
import java.util.HashMap;
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
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));

        if (itemIds == null || itemIds.isEmpty()) {
            return PurchaseItemsResponse.of(List.of(), 0, user.getPoint());
        }

        List<Item> items = getOrderedItems(itemIds);
        validateCategories(userId, items);

        int totalUsedPoints = items.stream()
                .mapToInt(Item::getPoint)
                .sum();

        if (user.getPoint() < totalUsedPoints) {
            throw new BaseException(ErrorCode.SHOP_INSUFFICIENT_POINTS);
        }

        user.usePoints(totalUsedPoints);
        savePurchasedItems(user, items);

        List<PurchaseItemResponse> purchasedItems = items.stream()
                .map(item -> PurchaseItemResponse.of(item, item.getDiscountRate()))
                .toList();

        return PurchaseItemsResponse.of(
                purchasedItems,
                totalUsedPoints,
                user.getPoint()
        );
    }

    @Transactional
    public void resetPurchasedItems(Integer userId) {
        itemUserRepository.resetPurchasedByUserId(userId);
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

    private void savePurchasedItems(User user, List<Item> items) {
        Map<Long, ItemUser> existingItemsByItemId = new HashMap<>();
        itemUserRepository.findAllByUser_IdAndItem_IdIn(
                        user.getId(),
                        items.stream().map(Item::getId).toList()
                )
                .forEach(itemUser -> existingItemsByItemId.put(itemUser.getItem().getId(), itemUser));

        List<ItemUser> itemUsersToSave = items.stream()
                .map(item -> {
                    ItemUser existingItemUser = existingItemsByItemId.get(item.getId());
                    if (existingItemUser != null) {
                        existingItemUser.markPurchased();
                        return existingItemUser;
                    }
                    return ItemUser.purchase(item, user);
                })
                .toList();

        itemUserRepository.saveAll(itemUsersToSave);
    }

    private void validateCategories(Integer userId, List<Item> items) {
        Set<ItemCategory> requestCategories = new LinkedHashSet<>();

        for (Item item : items) {
            if (!requestCategories.add(item.getCategory())) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "Only one item can be purchased per category in a single request."
                );
            }

            if (itemUserRepository.existsPurchasedCategoryItem(userId, item.getCategory())) {
                throw new BaseException(
                        ErrorCode.SHOP_ITEM_ALREADY_PURCHASED,
                        "An item in this category has already been purchased."
                );
            }
        }
    }
}
