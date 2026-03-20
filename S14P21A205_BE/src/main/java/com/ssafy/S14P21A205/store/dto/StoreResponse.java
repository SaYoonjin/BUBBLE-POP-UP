package com.ssafy.S14P21A205.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record StoreResponse(
        @Schema(description = "지역명") String location,
        @Schema(description = "팝업 이름") String popupName,
        @Schema(description = "메뉴명") String menu,
        @Schema(description = "DAY") Integer day,
        @Schema(name = "playableday", description = "플레이 가능 날짜 (중간 참여용)")
        @JsonProperty("playableday") Integer playableDay
) {
}
