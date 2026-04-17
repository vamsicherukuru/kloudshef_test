package com.kloudshef.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.kloudshef.backend.entity.DeviceToken;
import com.kloudshef.backend.repository.DeviceTokenRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;

    public FcmService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    // External path used on EC2 (written by CI/CD pipeline)
    @Value("${app.firebase.credentials-path:/opt/kloudshef/resources/firebase-service-account.json}")
    private String externalCredentialsPath;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) return;
        try {
            InputStream serviceAccount = loadCredentials();
            if (serviceAccount == null) {
                log.warn("firebase-service-account.json not found — push notifications disabled");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized");
        } catch (Exception e) {
            log.warn("Firebase Admin SDK init failed: {}", e.getMessage());
        }
    }

    private InputStream loadCredentials() {
        // 1. Try external file (production / EC2)
        File external = new File(externalCredentialsPath);
        if (external.exists()) {
            try {
                log.info("Loading Firebase credentials from external path: {}", externalCredentialsPath);
                return new FileInputStream(external);
            } catch (Exception e) {
                log.warn("Could not read external Firebase credentials: {}", e.getMessage());
            }
        }
        // 2. Fall back to classpath (local dev — file in src/main/resources/)
        ClassPathResource cp = new ClassPathResource("firebase-service-account.json");
        if (cp.exists()) {
            try {
                log.info("Loading Firebase credentials from classpath");
                return cp.getInputStream();
            } catch (Exception e) {
                log.warn("Could not read classpath Firebase credentials: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * Send notification to ALL devices of a user (multi-device support).
     */
    public void sendToUser(Long userId, String title, String body, String type) {
        sendToUser(userId, title, body, type, Map.of());
    }

    /**
     * Send notification to ALL devices with extra data.
     */
    public void sendToUser(Long userId, String title, String body, String type, Map<String, String> extraData) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.warn("FCM skip — no device tokens for userId [{}], type [{}]", userId, type);
            return;
        }
        log.info("FCM sending [{}] to {} device(s) for userId {}", type, tokens.size(), userId);
        for (DeviceToken dt : tokens) {
            sendNotification(dt.getFcmToken(), title, body, type, extraData);
        }
    }

    /**
     * Send notification to a single FCM token.
     */
    public void sendNotification(String fcmToken, String title, String body, String type) {
        sendNotification(fcmToken, title, body, type, Map.of());
    }

    public void sendNotification(String fcmToken, String title, String body, String type, Map<String, String> extraData) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM skip — no token for type [{}]", type);
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("FCM skip — Firebase not initialized");
                return;
            }
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(Map.of("type", type))
                    .putAllData(extraData)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setChannelId("kloudshef_orders")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .build())
                                    .setSound("default")
                                    .setBadge(1)
                                    .setContentAvailable(true)
                                    .build())
                            .putHeader("apns-priority", "10")
                            .putHeader("apns-push-type", "alert")
                            .build())
                    .setToken(fcmToken)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent [{}]: {}", type, response);
        } catch (FirebaseMessagingException e) {
            // Clean up stale/unregistered tokens automatically
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.info("FCM token unregistered — removing stale token");
                deviceTokenRepository.deleteByFcmToken(fcmToken);
            }
            log.warn("FCM send failed [{}] for token {}: {} (code={})",
                    type, fcmToken, e.getMessage(), e.getMessagingErrorCode());
        } catch (Exception e) {
            log.warn("FCM send failed [{}] for token {}: {}", type, fcmToken, e.getMessage());
        }
    }
}
