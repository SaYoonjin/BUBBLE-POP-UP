package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.StoreResponse;

public interface StoreService {

    StoreResponse getStore(Integer userId);
}