package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.request.UpdateProfileRequest;
import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.UserResponse;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final CookRepository cookRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal User user) {
        Long cookId = cookRepository.findByUserId(user.getId())
                .map(cook -> cook.getId())
                .orElse(null);

        UserResponse response = UserResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .city(user.getCity())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole().name())
                .cookId(cookId)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) managedUser.setFullName(request.getFullName());
        if (request.getPhone() != null) managedUser.setPhone(request.getPhone());
        if (request.getCity() != null) managedUser.setCity(request.getCity());
        if (request.getProfileImageUrl() != null) managedUser.setProfileImageUrl(request.getProfileImageUrl());

        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), managedUser.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            managedUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        User savedUser = userRepository.save(managedUser);

        Long cookId = cookRepository.findByUserId(savedUser.getId())
                .map(cook -> cook.getId())
                .orElse(null);

        UserResponse response = UserResponse.builder()
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .city(savedUser.getCity())
                .profileImageUrl(savedUser.getProfileImageUrl())
                .role(savedUser.getRole().name())
                .cookId(cookId)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }
}
