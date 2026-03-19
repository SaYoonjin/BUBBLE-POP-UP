package com.ssafy.S14P21A205.config;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Seoul");

    @PostConstruct
    void initializeDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APP_ZONE_ID));
    }

    @Bean
    public Clock systemClock() {
        return Clock.system(APP_ZONE_ID);
    }
}
