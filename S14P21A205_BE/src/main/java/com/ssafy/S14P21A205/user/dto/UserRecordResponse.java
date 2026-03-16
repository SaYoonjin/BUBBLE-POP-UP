package com.ssafy.S14P21A205.user.dto;

public record UserRecordResponse(
        Integer seasonNumber,
        Integer rank,
        String location,
        String popupName,
        Integer profit,
        Integer rewardPoint
) {
}