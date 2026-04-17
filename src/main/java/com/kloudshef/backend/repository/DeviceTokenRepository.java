package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /// Find all tokens for a user (multi-device)
    List<DeviceToken> findByUserId(Long userId);

    /// Find specific device for upsert
    Optional<DeviceToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    /// Delete stale token by FCM token value (when FCM returns UNREGISTERED)
    void deleteByFcmToken(String fcmToken);

    /// Delete all tokens for a user (on account deletion or logout-all)
    void deleteByUserId(Long userId);
}
