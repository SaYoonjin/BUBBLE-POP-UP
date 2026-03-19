package com.ssafy.S14P21A205.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class ClockConfigTests {

    private final ClockConfig clockConfig = new ClockConfig();

    @Test
    void systemClockUsesAsiaSeoulZone() {
        assertThat(clockConfig.systemClock().getZone()).isEqualTo(ClockConfig.APP_ZONE_ID);
    }

    @Test
    void initializeDefaultTimeZoneSetsAsiaSeoul() {
        TimeZone previous = TimeZone.getDefault();
        try {
            clockConfig.initializeDefaultTimeZone();
            assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ClockConfig.APP_ZONE_ID);
        } finally {
            TimeZone.setDefault(previous);
        }
    }
}
