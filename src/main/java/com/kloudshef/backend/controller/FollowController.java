package com.kloudshef.backend.controller;

import com.kloudshef.backend.dto.response.ApiResponse;
import com.kloudshef.backend.dto.response.FollowResponse;
import com.kloudshef.backend.dto.response.FollowerResponse;
import com.kloudshef.backend.repository.UserRepository;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.Follow;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.FollowRepository;
import com.kloudshef.backend.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowRepository followRepository;
    private final CookRepository cookRepository;
    private final FcmService fcmService;
    private final UserRepository userRepository;

    @PostMapping("/cook/{cookId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> follow(
            @PathVariable Long cookId,
            @AuthenticationPrincipal User user) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));
        if (followRepository.existsByUserIdAndCookId(user.getId(), cookId)) {
            throw new BadRequestException("Already following this kitchen");
        }
        Follow follow = Follow.builder().user(user).cook(cook).build();
        followRepository.save(follow);

        // Notify the cook about the new follower
        if (cook.getUser() != null) {
            long followerCount = followRepository.countByCookId(cookId);
            fcmService.sendToUser(
                    cook.getUser().getId(),
                    "New Follower! ❤️",
                    user.getFirstName() + " is now following " + cook.getKitchenName()
                            + " • " + followerCount + " follower" + (followerCount != 1 ? "s" : ""),
                    "new_follower"
            );
        }

        return ResponseEntity.ok(ApiResponse.success("Following kitchen", null));
    }

    @DeleteMapping("/cook/{cookId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable Long cookId,
            @AuthenticationPrincipal User user) {
        followRepository.deleteByUserIdAndCookId(user.getId(), cookId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed kitchen", null));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FollowResponse>>> myFollowing(
            @AuthenticationPrincipal User user) {
        List<FollowResponse> list = followRepository.findByUserId(user.getId()).stream()
                .map(f -> {
                    Cook c = f.getCook();
                    return FollowResponse.builder()
                            .cookId(c.getId())
                            .kitchenName(c.getKitchenName())
                            .kitchenHandle(c.getKitchenHandle())
                            .profileImageUrl(c.getProfileImageUrl())
                            .city(c.getCity())
                            .averageRating(c.getAverageRating())
                            .totalReviews(c.getTotalReviews())
                            .followedAt(f.getCreatedAt())
                            .build();
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/check/{cookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFollow(
            @PathVariable Long cookId,
            @AuthenticationPrincipal User user) {
        boolean following = followRepository.existsByUserIdAndCookId(user.getId(), cookId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("following", following)));
    }

    @GetMapping("/cook/{cookId}/followers")
    public ResponseEntity<ApiResponse<List<FollowerResponse>>> getFollowers(
            @PathVariable Long cookId) {
        cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));
        List<FollowerResponse> followers = followRepository.findByCookId(cookId).stream()
                .map(f -> {
                    User u = f.getUser();
                    return FollowerResponse.builder()
                            .userId(u.getId())
                            .fullName(u.getFullName())
                            .profileImageUrl(u.isDpPublic() ? u.getProfileImageUrl() : null)
                            .city(u.getCity())
                            .followedAt(f.getCreatedAt())
                            .build();
                }).toList();
        return ResponseEntity.ok(ApiResponse.success(followers));
    }

    @GetMapping("/cook/{cookId}/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getFollowerCount(
            @PathVariable Long cookId) {
        long count = followRepository.countByCookId(cookId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }
}
