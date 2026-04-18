package com.kloudshef.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cooks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String kitchenName;

    @Column(unique = true)
    private String kitchenHandle;

    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String kitchenType;

    private String cookingStyle;

    private String specialties;

    private String city;

    private String area;

    private String country; // ISO-3166 alpha-2, e.g. "IN", "US"

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

    @Column(columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(columnDefinition = "TEXT")
    private String reviewSummary;

    @Builder.Default
    private Integer totalOrders = 0;

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalReviews = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.TRIAL;

    @OneToMany(mappedBy = "cook", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();

    @OneToMany(mappedBy = "cook", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
