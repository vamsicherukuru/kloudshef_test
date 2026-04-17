package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.request.PlaceOrderRequest;
import com.kloudshef.backend.dto.request.UpdateOrderStatusRequest;
import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.OrderResponse;
import com.kloudshef.backend.entity.Role;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CookRepository cookRepository;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> placeOrder(
            @AuthenticationPrincipal User user,
            @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.placeOrder(user.getId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(@AuthenticationPrincipal User user) {
        if (user.getRole() == Role.COOK) {
            Long cookId = cookRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new com.kloudshef.backend.exception.ResourceNotFoundException("Cook profile not found"))
                    .getId();
            return ResponseEntity.ok(ApiResponse.success(orderService.getCookOrders(cookId)));
        }
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(user.getId())));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @RequestBody UpdateOrderStatusRequest request) {
        Long cookId = cookRepository.findByUserId(user.getId())
                .orElseThrow(() -> new com.kloudshef.backend.exception.ResourceNotFoundException("Cook profile not found"))
                .getId();
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, cookId, request.getStatus(), request.getEstimatedPickupMinutes())));
    }
}
