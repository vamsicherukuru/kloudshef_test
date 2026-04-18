package com.kloudshef.backend.dto.response;

import com.kloudshef.backend.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CookSummaryResponse {
    private Long id;
    private String fullName;
    private String kitchenName;
    private String cookingStyle;
    private String kitchenType;
    private String specialties;
    private String city;
    private String area;
    private String country;
    private String profileImageUrl;
    private String coverImageUrl;
    private String kitchenImageUrl;
    private Double averageRating;
    private Integer totalReviews;
    private SubscriptionStatus subscriptionStatus;
    private String availableDays;
    private String availableHours;
    private Integer totalOrders;
    private Double distanceKm;
    private Long followerCount;
}
