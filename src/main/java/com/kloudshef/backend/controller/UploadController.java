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

    @Value("${app.upload.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No file provided"));
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        Files.write(dir.resolve(filename), file.getBytes());

        String url = appBaseUrl.replaceAll("/+$", "") + "/uploads/images/" + filename;
        return ResponseEntity.ok(ApiResponse.success("Uploaded", Map.of("url", url)));
    }
}
