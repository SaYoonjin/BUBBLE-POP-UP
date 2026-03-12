package com.ssafy.S14P21A205.store.service;

import com.ssafy.S14P21A205.store.dto.StoreResponse;
import java.util.UUID;

public interface StoreService {

    StoreResponse getStore(UUID userId);
}