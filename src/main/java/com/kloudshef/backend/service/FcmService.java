package com.kloudshef.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kloudshef.backend.entity.DeviceToken;
import com.kloudshef.backend.repository.DeviceTokenRepository;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;
    private GoogleCredentials credentials;
    private String projectId;

    public FcmService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Value("${app.firebase.credentials-path:/opt/kloudshef/resources/firebase-service-account.json}")
    private String externalCredentialsPath;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = loadCredentials();
            if (serviceAccount == null) {
                log.warn("firebase-service-account.json not found — push notifications disabled");
                return;
            }
            byte[] bytes = serviceAccount.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            int idx = json.indexOf("\"project_id\"");
            if (idx >= 0) {
                int colon = json.indexOf(":", idx);
                int quote1 = json.indexOf("\"", colon + 1);
                int quote2 = json.indexOf("\"", quote1 + 1);
                projectId = json.substring(quote1 + 1, quote2);
            }
            credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(bytes))
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));
            credentials.refreshIfExpired();
            log.info("FCM initialized — project: {}, access token obtained successfully", projectId);
        } catch (Exception e) {
            log.warn("FCM init failed: {}", e.getMessage(), e);
        }
    }

    private InputStream loadCredentials() {
        File external = new File(externalCredentialsPath);
        if (external.exists()) {
            try {
                log.info("Loading Firebase credentials from external path: {}", externalCredentialsPath);
                return new FileInputStream(external);
            } catch (Exception e) {
                log.warn("Could not read external Firebase credentials: {}", e.getMessage());
            }
        }
        try {
            InputStream cp = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (cp != null) {
                log.info("Loading Firebase credentials from classpath");
                return cp;
            }
        } catch (Exception e) {
            log.warn("Could not read classpath Firebase credentials: {}", e.getMessage());
        }
        return null;
    }

    private String getAccessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    public void sendToUser(Long userId, String title, String body, String type) {
        sendToUser(userId, title, body, type, Map.of());
    }

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

    public void sendNotification(String fcmToken, String title, String body, String type) {
        sendNotification(fcmToken, title, body, type, Map.of());
    }

    public void sendNotification(String fcmToken, String title, String body, String type, Map<String, String> extraData) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM skip — no token for type [{}]", type);
            return;
        }
        if (credentials == null || projectId == null) {
            log.warn("FCM skip — not initialized");
            return;
        }
        try {
            String accessToken = getAccessToken();
            String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

            StringBuilder dataJson = new StringBuilder();
            dataJson.append("\"type\":\"").append(escapeJson(type)).append("\"");
            for (Map.Entry<String, String> entry : extraData.entrySet()) {
                dataJson.append(",\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
            }

            String payload = "{\"message\":{\"token\":\"" + escapeJson(fcmToken) + "\","
                + "\"notification\":{\"title\":\"" + escapeJson(title) + "\",\"body\":\"" + escapeJson(body) + "\"},"
                + "\"data\":{" + dataJson.toString() + "},"
                + "\"android\":{\"priority\":\"HIGH\",\"notification\":{\"sound\":\"default\",\"channel_id\":\"kloudshef_orders\"}},"
                + "\"apns\":{\"headers\":{\"apns-priority\":\"10\",\"apns-push-type\":\"alert\"},"
                + "\"payload\":{\"aps\":{\"alert\":{\"title\":\"" + escapeJson(title) + "\",\"body\":\"" + escapeJson(body) + "\"},"
                + "\"sound\":\"default\",\"badge\":1,\"content-available\":1}}}}}";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String resp = readStream(conn.getInputStream());
                log.info("FCM sent [{}]: {}", type, resp);
            } else {
                String error = readStream(conn.getErrorStream());
                log.warn("FCM send failed [{}] for token {}: HTTP {} — {}", type, fcmToken, responseCode, error);
                if (error.contains("UNREGISTERED") || error.contains("NOT_FOUND")) {
                    log.info("FCM token unregistered — removing stale token");
                    deviceTokenRepository.deleteByFcmToken(fcmToken);
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("FCM send failed [{}] for token {}: {}", type, fcmToken, e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String readStream(InputStream is) {
        if (is == null) return "null";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "error reading stream";
        }
    }
}
