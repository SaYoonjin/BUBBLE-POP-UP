package com.ssafy.S14P21A205.store.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class StoreResponseTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesPlayableDayAsRequestedFieldName() throws Exception {
        StoreResponse response = new StoreResponse(
                "성수",
                "PulsePop Kitchen",
                "치킨 타코",
                3,
                3
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("location").asText()).isEqualTo("성수");
        assertThat(json.get("popupName").asText()).isEqualTo("PulsePop Kitchen");
        assertThat(json.get("menu").asText()).isEqualTo("치킨 타코");
        assertThat(json.get("day").asInt()).isEqualTo(3);
        assertThat(json.has("playableday")).isTrue();
        assertThat(json.has("playableDay")).isFalse();
        assertThat(json.get("playableday").asInt()).isEqualTo(3);
    }
}
