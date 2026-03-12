package com.ssafy.S14P21A205.shop.service;

import com.ssafy.S14P21A205.shop.dto.ShopItemListResponse;
import com.ssafy.S14P21A205.shop.dto.ShopItemResponse;
import com.ssafy.S14P21A205.shop.entity.Item;
import com.ssafy.S14P21A205.shop.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ItemRepository itemRepository;

    public ShopItemListResponse getShopItems() {

        List<Item> items = itemRepository.findAllByOrderByIdAsc();

        List<ShopItemResponse> itemResponses = items.stream()
                .map(ShopItemResponse::from)
                .toList();

        return ShopItemListResponse.of(itemResponses);
    }
}