package com.ssafy.S14P21A205.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.S14P21A205.auth.service.AuthService;
import com.ssafy.S14P21A205.auth.service.JwtTokenService;
import com.ssafy.S14P21A205.game.day.dto.GameDayStartResponse;
import com.ssafy.S14P21A205.game.day.service.GameDayService;
import com.ssafy.S14P21A205.shop.service.ShopService;
import com.ssafy.S14P21A205.store.service.StoreService;
import com.ssafy.S14P21A205.user.service.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "app.jwt.secret=test-jwt-secret-key-that-is-long-enough-1234"
})
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GameDayService gameDayService;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private StoreService storeService;

    @Test
    void startDayAllowsAuthenticatedRequestWithCsrfHeader() throws Exception {
        when(gameDayService.startDay(any(), any()))
                .thenReturn(new GameDayStartResponse(
                        "10:00",
                        "22:00",
                        Map.of(),
                        "SUNNY",
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        List.of(),
                        10_000_000,
                        100
                ));

        mockMvc.perform(post("/game/day/start")
                        .with(jwt().jwt(jwt -> jwt.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": 99990001,
                                  "menuId": 99990001,
                                  "price": 5000,
                                  "orderCount": 100
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void startDayReturnsDetailedMessageWhenRequestValidationFails() throws Exception {
        mockMvc.perform(post("/game/day/start")
                        .with(jwt().jwt(jwt -> jwt.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": 99990001,
                                  "menuId": 99990001,
                                  "price": 0,
                                  "orderCount": 100
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-002"))
                .andExpect(jsonPath("$.message").value("price는 0보다 커야 합니다."));
    }
}
