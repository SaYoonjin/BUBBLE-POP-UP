package com.ssafy.S14P21A205.game.environment.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class WeatherLocationId implements Serializable {

    private Long location;
    private Integer day;
}
