package com.kloudshef.backend.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CookProfileRequest {
    private LocalDate dateOfBirth;
    private String kitchenName;
    private String bio;
    private String kitchenType;
    private String cookingStyle;
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
    private String availableDays;
    private String availableHours;
    private String welcomeMessage;
}
