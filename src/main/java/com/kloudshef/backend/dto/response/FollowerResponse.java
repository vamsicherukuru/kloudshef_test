package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FollowerResponse {
    private Long userId;
    private String fullName;
    private String profileImageUrl;
    private String city;
    private LocalDateTime followedAt;
}
