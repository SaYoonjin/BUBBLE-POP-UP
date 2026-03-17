package com.ssafy.S14P21A205.game.time.model;

import java.time.LocalDateTime;

public record DayWindow(
        LocalDateTime dayStart,
        LocalDateTime businessStart,
        LocalDateTime businessEnd,
        LocalDateTime reportEnd,
        LocalDateTime seasonPlayableEnd
) {
}
