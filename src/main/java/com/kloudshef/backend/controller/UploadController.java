package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No file provided"));
        }

        // Save file under uploads/images/
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        Files.write(dir.resolve(filename), file.getBytes());

        // Build URL dynamically from the incoming request — works local and on EC2
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        int port = request.getServerPort();
        if (!((request.getScheme().equals("http") && port == 80)
                || (request.getScheme().equals("https") && port == 443))) {
            baseUrl += ":" + port;
        }

        String url = baseUrl + "/uploads/images/" + filename;
        return ResponseEntity.ok(ApiResponse.success("Uploaded", Map.of("url", url)));
    }
}
