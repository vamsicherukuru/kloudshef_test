package com.kloudshef.backend.dto.response;

import com.kloudshef.backend.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CookDetailResponse {
    private Long id;
    private String fullName;
    private String kitchenName;
    private String bio;
    private String cookingStyle;
    private String kitchenType;
    private String specialties;
    private String city;
    private String area;
    private String country;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String whatsappNumber;
    private String profileImageUrl;
    private String coverImageUrl;
    private String kitchenImageUrl;
    private Double averageRating;
    private Integer totalReviews;
    private SubscriptionStatus subscriptionStatus;
    private String availableDays;
    private String availableHours;
    private List<MenuItemResponse> menuItems;
    private String welcomeMessage;
    private Integer totalOrders;
    private LocalDate dateOfBirth;
    private String reviewSummary;
    private Long followerCount;
}
