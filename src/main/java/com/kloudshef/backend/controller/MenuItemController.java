package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.request.MenuItemRequest;
import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.MenuItemResponse;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/cook/{cookId}")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenuItems(@PathVariable Long cookId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getMenuItemsByCookId(cookId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu item added", menuItemService.addMenuItem(user.getId(), request)));
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", menuItemService.updateMenuItem(user.getId(), itemId, request)));
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('COOK')")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId) {
        menuItemService.deleteMenuItem(user.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
