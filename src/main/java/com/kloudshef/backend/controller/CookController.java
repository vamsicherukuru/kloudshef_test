package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.request.CookProfileRequest;
import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.CookDetailResponse;
import com.kloudshef.backend.dto.response.CookSummaryResponse;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.service.CookService;
import com.kloudshef.backend.service.HuggingFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cooks")
@RequiredArgsConstructor
public class CookController {

    private final CookService cookService;
    private final HuggingFaceService huggingFaceService;

    @GetMapping("/browse")
    public ResponseEntity<ApiResponse<Page<CookSummaryResponse>>> browseCooks(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String cookingStyle,
            @RequestParam(required = false) String kitchenType,
            @RequestParam(required = false) String specialties,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                cookService.browseCooks(city, lat, lng, radiusKm,
                        cookingStyle, kitchenType, specialties, minRating,
                        PageRequest.of(page, size, Sort.by("averageRating").descending()))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CookSummaryResponse>>> searchCooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                cookService.searchCooks(query, PageRequest.of(page, size))));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<String>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(cookService.getAvailableCities()));
    }

    @GetMapping("/handle-available")
    public ResponseEntity<ApiResponse<Boolean>> checkHandleAvailable(@RequestParam String handle) {
        String clean = handle.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        boolean available = !clean.isEmpty() && clean.length() >= 3 && !cookService.isHandleTaken(clean);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    @GetMapping("/by-handle/{handle}")
    public ResponseEntity<ApiResponse<CookSummaryResponse>> getByHandle(@PathVariable String handle) {
        return ResponseEntity.ok(ApiResponse.success(cookService.getCookByHandle(handle)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CookDetailResponse>> getCookById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cookService.getCookById(id)));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<ApiResponse<List<CookSummaryResponse>>> getSimilarCooks(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cookService.getSimilarCooks(id)));
    }

    @GetMapping("/{id}/review-summary")
    public ResponseEntity<ApiResponse<String>> getReviewSummary(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(huggingFaceService.generateSummary(id)));
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<CookDetailResponse>> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(cookService.getCookByUserId(user.getId())));
    }

    @PutMapping("/my-profile")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<CookDetailResponse>> updateMyProfile(
            @AuthenticationPrincipal User user,
            @RequestBody CookProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                cookService.updateCookProfile(user.getId(), request)));
    }

    @PutMapping("/my-profile/availability")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<CookDetailResponse>> toggleAvailability(
            @AuthenticationPrincipal User user,
            @RequestBody java.util.Map<String, Boolean> body) {
        boolean availableNow = Boolean.TRUE.equals(body.get("availableNow"));
        return ResponseEntity.ok(ApiResponse.success(
                availableNow ? "You're now available" : "You're now offline",
                cookService.toggleAvailability(user.getId(), availableNow)));
    }
}
