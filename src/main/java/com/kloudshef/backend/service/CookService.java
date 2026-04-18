package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.CookProfileRequest;
import com.kloudshef.backend.dto.response.CookDetailResponse;
import com.kloudshef.backend.dto.response.CookSummaryResponse;
import com.kloudshef.backend.dto.response.MenuItemResponse;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.SubscriptionStatus;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CookService {

    private final CookRepository cookRepository;
    private final FollowRepository followRepository;
    private final FcmService fcmService;

    private static final List<SubscriptionStatus> VISIBLE_STATUSES =
            Arrays.asList(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL);

    public Page<CookSummaryResponse> browseCooks(String city, Double userLat, Double userLng,
                                                  Double radiusKm, String cookingStyle,
                                                  String kitchenType, String specialties,
                                                  Double minRating, Pageable pageable) {
        // Build a base stream, then apply filters in memory for flexibility
        List<Cook> all;
        if (city != null && !city.isBlank()) {
            all = (userLat != null && userLng != null)
                    ? cookRepository.findAllByStatusesAndCity(VISIBLE_STATUSES, city)
                    : new ArrayList<>(cookRepository.findByStatusesAndCity(VISIBLE_STATUSES, city, pageable).getContent());
        } else {
            all = (userLat != null && userLng != null)
                    ? cookRepository.findAllByStatuses(VISIBLE_STATUSES)
                    : new ArrayList<>(cookRepository.findByStatuses(VISIBLE_STATUSES, Pageable.unpaged()).getContent());
        }

        // Apply quick filters
        var stream = all.stream();
        if (cookingStyle != null && !cookingStyle.isBlank()) {
            final String cs = cookingStyle.toLowerCase();
            stream = stream.filter(c -> {
                String s = c.getCookingStyle();
                String sp = c.getSpecialties();
                return (s != null && s.toLowerCase().contains(cs))
                    || (sp != null && sp.toLowerCase().contains(cs));
            });
        }
        if (kitchenType != null && !kitchenType.isBlank()) {
            final String kt = kitchenType.toLowerCase();
            stream = stream.filter(c -> {
                String t = c.getKitchenType();
                return t != null && t.toLowerCase().contains(kt);
            });
        }
        if (specialties != null && !specialties.isBlank()) {
            final String sp = specialties.toLowerCase();
            stream = stream.filter(c -> {
                String s = c.getSpecialties();
                String cs2 = c.getCookingStyle();
                String kn = c.getKitchenName();
                return (s != null && s.toLowerCase().contains(sp))
                    || (cs2 != null && cs2.toLowerCase().contains(sp))
                    || (kn != null && kn.toLowerCase().contains(sp));
            });
        }
        if (minRating != null) {
            stream = stream.filter(c -> c.getAverageRating() >= minRating);
        }

        // Compute distances if user location available
        List<CookSummaryResponse> results;
        if (userLat != null && userLng != null) {
            List<CookSummaryResponse> withDist = stream
                    .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                    .map(c -> {
                        double d = haversine(userLat, userLng, c.getLatitude(), c.getLongitude());
                        return toSummary(c, d);
                    })
                    .filter(c -> radiusKm == null || c.getDistanceKm() <= radiusKm)
                    .sorted(Comparator.comparingDouble(CookSummaryResponse::getDistanceKm))
                    .collect(Collectors.toList());

            // Cooks without coordinates appended at end (only when no radius filter)
            if (radiusKm == null) {
                all.stream()
                        .filter(c -> c.getLatitude() == null || c.getLongitude() == null)
                        .map(c -> toSummary(c, null))
                        .forEach(withDist::add);
            }
            results = withDist;
        } else {
            results = stream.map(c -> toSummary(c, null)).collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), results.size());
        List<CookSummaryResponse> page = start >= results.size()
                ? new ArrayList<>() : results.subList(start, end);
        return new PageImpl<>(page, pageable, results.size());
    }

    /** Haversine formula — returns distance in kilometres. */
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public Page<CookSummaryResponse> searchCooks(String query, Pageable pageable) {
        return cookRepository.searchActiveCooks(VISIBLE_STATUSES, query, pageable)
                .map(c -> toSummary(c, null));
    }

    public CookDetailResponse getCookById(Long id) {
        Cook cook = cookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found with id: " + id));
        return toDetail(cook);
    }

    public CookDetailResponse getCookByUserId(Long userId) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));
        return toDetail(cook);
    }

    @Transactional
    public CookDetailResponse updateCookProfile(Long userId, CookProfileRequest request) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));

        if (request.getKitchenName() != null) cook.setKitchenName(request.getKitchenName());
        if (request.getKitchenHandle() != null) {
            String handle = request.getKitchenHandle().trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
            if (!handle.isEmpty() && !handle.equals(cook.getKitchenHandle())) {
                if (cookRepository.existsByKitchenHandle(handle)) {
                    throw new com.kloudshef.backend.exception.BadRequestException("Handle @" + handle + " is already taken");
                }
                cook.setKitchenHandle(handle);
            }
        }
        if (request.getBio() != null) cook.setBio(request.getBio());
        if (request.getKitchenType() != null) cook.setKitchenType(request.getKitchenType());
        if (request.getCookingStyle() != null) cook.setCookingStyle(request.getCookingStyle());
        if (request.getSpecialties() != null) cook.setSpecialties(request.getSpecialties());
        if (request.getCity() != null) cook.setCity(request.getCity());
        if (request.getArea() != null) cook.setArea(request.getArea());
        if (request.getCountry() != null) cook.setCountry(request.getCountry());
        if (request.getAddress() != null) cook.setAddress(request.getAddress());
        if (request.getLatitude() != null) cook.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) cook.setLongitude(request.getLongitude());
        if (request.getPhone() != null) cook.setPhone(request.getPhone());
        if (request.getWhatsappNumber() != null) cook.setWhatsappNumber(request.getWhatsappNumber());
        if (request.getProfileImageUrl() != null) cook.setProfileImageUrl(request.getProfileImageUrl());
        if (request.getCoverImageUrl() != null) cook.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getKitchenImageUrl() != null) cook.setKitchenImageUrl(request.getKitchenImageUrl());
        if (request.getAvailableDays() != null) cook.setAvailableDays(request.getAvailableDays());
        if (request.getAvailableHours() != null) cook.setAvailableHours(request.getAvailableHours());
        if (request.getAvailableNow() != null) cook.setAvailableNow(request.getAvailableNow());
        if (request.getWelcomeMessage() != null) cook.setWelcomeMessage(request.getWelcomeMessage());
        if (request.getDateOfBirth() != null) cook.setDateOfBirth(request.getDateOfBirth());

        return toDetail(cookRepository.save(cook));
    }

    @Transactional
    public CookDetailResponse toggleAvailability(Long userId, boolean availableNow) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));
        cook.setAvailableNow(availableNow);
        cook = cookRepository.save(cook);

        // Notify followers when cook goes online
        if (availableNow) {
            List<Long> followerIds = followRepository.findFollowerUserIdsByCookId(cook.getId());
            String title = cook.getKitchenName() + " is now available!";
            String body = "Order fresh homemade food from " + cook.getKitchenName() + " while they're online.";
            for (Long followerId : followerIds) {
                fcmService.sendToUser(followerId, title, body, "cook_available",
                        java.util.Map.of("cookId", cook.getId().toString()));
            }
        }

        return toDetail(cook);
    }

    public List<String> getAvailableCities() {
        return cookRepository.findDistinctActiveCities();
    }

    public boolean isHandleTaken(String handle) {
        return cookRepository.existsByKitchenHandle(handle);
    }

    public CookSummaryResponse getCookByHandle(String handle) {
        Cook cook = cookRepository.findByKitchenHandle(handle.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Kitchen not found with handle: @" + handle));
        return toSummary(cook, null);
    }

    public List<CookSummaryResponse> getSimilarCooks(Long cookId) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));
        String city = cook.getCity() != null ? cook.getCity() : "";
        String style = cook.getCookingStyle() != null ? cook.getCookingStyle() : "";
        return cookRepository.findSimilarCooks(
                        VISIBLE_STATUSES, cookId, city, style, PageRequest.of(0, 6))
                .stream()
                .map(c -> toSummary(c, null))
                .collect(Collectors.toList());
    }

    private CookSummaryResponse toSummary(Cook cook, Double distanceKm) {
        Double rounded = distanceKm != null
                ? Math.round(distanceKm * 10.0) / 10.0 : null;
        return CookSummaryResponse.builder()
                .id(cook.getId())
                .fullName(cook.getUser() != null ? cook.getUser().getFullName() : null)
                .kitchenName(cook.getKitchenName())
                .kitchenHandle(cook.getKitchenHandle())
                .cookingStyle(cook.getCookingStyle())
                .kitchenType(cook.getKitchenType())
                .specialties(cook.getSpecialties())
                .city(cook.getCity())
                .area(cook.getArea())
                .country(cook.getCountry())
                .profileImageUrl(cook.getProfileImageUrl() != null ? cook.getProfileImageUrl() : (cook.getUser() != null ? cook.getUser().getProfileImageUrl() : null))
                .coverImageUrl(cook.getCoverImageUrl())
                .kitchenImageUrl(cook.getKitchenImageUrl())
                .averageRating(cook.getAverageRating())
                .totalReviews(cook.getTotalReviews())
                .subscriptionStatus(cook.getSubscriptionStatus())
                .availableDays(cook.getAvailableDays())
                .availableHours(cook.getAvailableHours())
                .availableNow(cook.getAvailableNow() != null ? cook.getAvailableNow() : false)
                .totalOrders(cook.getTotalOrders())
                .distanceKm(rounded)
                .followerCount(followRepository.countByCookId(cook.getId()))
                .build();
    }

    private CookDetailResponse toDetail(Cook cook) {
        List<MenuItemResponse> menuItems = cook.getMenuItems().stream()
                .map(item -> MenuItemResponse.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .price(item.getPrice())
                        .category(item.getCategory())
                        .imageUrl(item.getImageUrl())
                        .tags(item.getTags())
                        .available(item.isAvailable())
                        .vegetarian(item.isVegetarian())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();

        return CookDetailResponse.builder()
                .id(cook.getId())
                .fullName(cook.getUser() != null ? cook.getUser().getFullName() : null)
                .kitchenName(cook.getKitchenName())
                .kitchenHandle(cook.getKitchenHandle())
                .bio(cook.getBio())
                .cookingStyle(cook.getCookingStyle())
                .kitchenType(cook.getKitchenType())
                .specialties(cook.getSpecialties())
                .city(cook.getCity())
                .area(cook.getArea())
                .country(cook.getCountry())
                .address(cook.getAddress())
                .latitude(cook.getLatitude())
                .longitude(cook.getLongitude())
                .phone(cook.getPhone())
                .whatsappNumber(cook.getWhatsappNumber())
                .profileImageUrl(cook.getProfileImageUrl() != null ? cook.getProfileImageUrl() : (cook.getUser() != null ? cook.getUser().getProfileImageUrl() : null))
                .coverImageUrl(cook.getCoverImageUrl())
                .kitchenImageUrl(cook.getKitchenImageUrl())
                .averageRating(cook.getAverageRating())
                .totalReviews(cook.getTotalReviews())
                .subscriptionStatus(cook.getSubscriptionStatus())
                .availableDays(cook.getAvailableDays())
                .availableHours(cook.getAvailableHours())
                .availableNow(cook.getAvailableNow() != null ? cook.getAvailableNow() : false)
                .menuItems(menuItems)
                .welcomeMessage(cook.getWelcomeMessage())
                .totalOrders(cook.getTotalOrders())
                .dateOfBirth(cook.getDateOfBirth())
                .reviewSummary(cook.getReviewSummary())
                .followerCount(followRepository.countByCookId(cook.getId()))
                .build();
    }
}
