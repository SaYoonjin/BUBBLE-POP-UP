package com.ssafy.S14P21A205.store.dto;

import java.util.List;

public record LocationListResponse(
        List<LocationResponse> locations
) {
}