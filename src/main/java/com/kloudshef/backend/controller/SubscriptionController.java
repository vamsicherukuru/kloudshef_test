package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.entity.Subscription;
import com.kloudshef.backend.entity.SubscriptionStatus;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CookRepository cookRepository;

    @PostMapping("/activate")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<Subscription>> activate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String paymentReference) {
        Long cookId = cookRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found")).getId();
        return ResponseEntity.ok(ApiResponse.success("Subscription activated",
                subscriptionService.activateSubscription(cookId, paymentReference)));
    }

    @GetMapping("/my-subscriptions")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<List<Subscription>>> getMySubscriptions(@AuthenticationPrincipal User user) {
        Long cookId = cookRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found")).getId();
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getCookSubscriptions(cookId)));
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<SubscriptionStatus>> getStatus(@AuthenticationPrincipal User user) {
        Long cookId = cookRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found")).getId();
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getCookSubscriptionStatus(cookId)));
    }
}
