package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://192.168.1.7:8080}")
    private String baseUrl;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No file provided"));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only image files are allowed"));
        }

        // Determine extension
        String ext = switch (contentType) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };

        // Save file
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ext;
        Files.write(dir.resolve(filename), file.getBytes());

        String url = baseUrl + "/uploads/images/" + filename;
        return ResponseEntity.ok(ApiResponse.success("Uploaded", Map.of("url", url)));
    }
}
