package com.kloudshef.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /// Unique per device — e.g. "ios_<identifierForVendor>" or "android_<androidId>"
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /// FCM registration token (changes on app reinstall, token refresh, etc.)
    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(nullable = false)
    private String platform; // "ios" or "android"

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUsedAt = LocalDateTime.now();
    }
}
