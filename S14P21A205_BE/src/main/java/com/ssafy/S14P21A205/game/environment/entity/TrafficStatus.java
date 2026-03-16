package com.ssafy.S14P21A205.game.environment.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrafficStatus {
    VERY_SMOOTH(1),
    SMOOTH(2),
    NORMAL(3),
    CONGESTED(4),
    VERY_CONGESTED(5);

    private final int value;
}
