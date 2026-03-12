package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.LocationListResponse;
import com.ssafy.S14P21A205.store.dto.StoreResponse;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationRequest;
import com.ssafy.S14P21A205.store.dto.UpdateStoreLocationResponse;

public interface StoreService {

    StoreResponse getStore(Long userId);

    UpdateStoreLocationResponse updateStoreLocation(Long userId, UpdateStoreLocationRequest request);

    LocationListResponse getLocations(Long userId);
}