package com.kloudshef.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class FcmService {

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

    public void sendNotification(String fcmToken, String title, String body, String type) {
        if (fcmToken == null || fcmToken.isBlank()) return;
        try {
            if (FirebaseApp.getApps().isEmpty()) return;
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(Map.of("type", type))
                    .setToken(fcmToken)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM sent [{}]: {}", type, response);
        } catch (Exception e) {
            log.warn("FCM send failed for token {}: {}", fcmToken, e.getMessage());
        }
    }
}
