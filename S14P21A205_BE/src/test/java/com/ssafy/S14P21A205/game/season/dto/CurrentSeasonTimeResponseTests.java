package com.ssafy.S14P21A205.game.season.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CurrentSeasonTimeResponseTests {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serializesNullableFieldsAsExplicitNulls() throws Exception {
        CurrentSeasonTimeResponse response = new CurrentSeasonTimeResponse(
                "DAY_PREPARING",
                3,
                12,
                LocalDateTime.of(2026, 3, 18, 12, 5, 10),
                LocalDateTime.of(2026, 3, 18, 12, 0, 0),
                null,
                null,
                true,
                null
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("seasonPhase").asText()).isEqualTo("DAY_PREPARING");
        assertThat(json.get("currentDay").asInt()).isEqualTo(3);
        assertThat(json.has("gameTime")).isTrue();
        assertThat(json.get("gameTime").isNull()).isTrue();
        assertThat(json.has("tick")).isTrue();
        assertThat(json.get("tick").isNull()).isTrue();
        assertThat(json.has("joinPlayableFromDay")).isTrue();
        assertThat(json.get("joinPlayableFromDay").isNull()).isTrue();
    }
}
