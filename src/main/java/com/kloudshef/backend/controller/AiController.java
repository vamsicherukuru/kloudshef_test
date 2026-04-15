package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.service.HuggingFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final HuggingFaceService huggingFaceService;

    @PostMapping("/dish-description")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<String>> generateDishDescription(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String result = huggingFaceService.generateDishDescription(
                body.get("name"),
                body.get("kitchenType"),
                body.get("cookingStyle"));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/kitchen-bio")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<String>> generateKitchenBio(
            @AuthenticationPrincipal User user) {
        String result = huggingFaceService.generateKitchenBio(user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/dish-tags")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<List<String>>> generateDishTags(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        List<String> tags = huggingFaceService.generateDishTags(
                body.get("name"),
                body.get("description"));
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
}
