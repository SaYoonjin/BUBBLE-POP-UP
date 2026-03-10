package com.ssafy.S14P21A205.game.environment.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WeatherMenuId implements Serializable {

    private Long weather;
    private Long menu;
}
