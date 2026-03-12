package com.ssafy.S14P21A205.shop.service;

import com.ssafy.S14P21A205.shop.dto.PurchasedItemListResponse;
import com.ssafy.S14P21A205.shop.dto.PurchasedItemResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemResponse;
import com.ssafy.S14P21A205.shop.entity.Item;
import com.ssafy.S14P21A205.shop.entity.ItemUser;
import com.ssafy.S14P21A205.shop.repository.ItemRepository;
import java.util.List;

import com.ssafy.S14P21A205.shop.repository.ItemUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ItemRepository itemRepository;
    private final ItemUserRepository itemUserRepository;

    /**
     * 상점에서 구매 가능한 전체 아이템 목록 조회
     */
    public ShopItemListResponse getShopItems() {

        List<Item> items = itemRepository.findAllByOrderByIdAsc();

        List<ShopItemResponse> itemResponses = items.stream()
                .map(ShopItemResponse::from)
                .toList();

        return ShopItemListResponse.of(itemResponses);
    }

    /**
     * 현재 시즌에서 구매한 아이템 목록 조회
     */
    public PurchasedItemListResponse getPurchasedItems(Long userId) {
        List<ItemUser> purchasedItems = itemUserRepository.findPurchasedItemsByUserId(userId);

        List<PurchasedItemResponse> responses = purchasedItems.stream()
                .map(itemUser -> PurchasedItemResponse.of(
                        itemUser.getItem().getId(),
                        itemUser.getItem().getDiscountRate()
                ))
                .toList();

        return PurchasedItemListResponse.of(responses);
    }
}