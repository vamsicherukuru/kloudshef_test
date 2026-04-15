package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.request.ReviewRequest;
import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.ReviewResponse;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/cook/{cookId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable Long cookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReviewsByCookId(cookId, PageRequest.of(page, size))));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getMyReviews(user.getId())));
    }

    @PostMapping("/cook/{cookId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long cookId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review submitted",
                reviewService.addReview(cookId, user.getId(), request)));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> editReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Review updated",
                reviewService.editReview(reviewId, user.getId(), request)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user) {
        reviewService.deleteReview(reviewId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
