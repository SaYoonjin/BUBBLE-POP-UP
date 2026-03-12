package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.*;

public interface StoreService {

    StoreResponse getStore(Integer userId);

    UpdateStoreLocationResponse updateStoreLocation(Integer userId, UpdateStoreLocationRequest request);

    LocationListResponse getLocations(Integer userId);

    MenuListResponse getMenus(Integer userId);
}
