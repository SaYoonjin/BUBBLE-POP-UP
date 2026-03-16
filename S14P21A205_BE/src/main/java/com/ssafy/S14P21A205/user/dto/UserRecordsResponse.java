package com.ssafy.S14P21A205.user.dto;

import java.util.List;

public record UserRecordsResponse(
        List<UserRecordResponse> records
) {
}