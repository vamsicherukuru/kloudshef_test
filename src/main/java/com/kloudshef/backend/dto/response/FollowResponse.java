package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FollowResponse {
    private Long cookId;
    private String kitchenName;
    private String profileImageUrl;
    private String city;
    private Double averageRating;
    private Integer totalReviews;
    private LocalDateTime followedAt;
}
