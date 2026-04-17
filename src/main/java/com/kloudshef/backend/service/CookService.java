package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.CookProfileRequest;
import com.kloudshef.backend.dto.response.CookDetailResponse;
import com.kloudshef.backend.dto.response.CookSummaryResponse;
import com.kloudshef.backend.dto.response.MenuItemResponse;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.SubscriptionStatus;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
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

    private static final List<SubscriptionStatus> VISIBLE_STATUSES =
            Arrays.asList(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL);

    public Page<CookSummaryResponse> browseCooks(String city, Double userLat, Double userLng,
                                                  Double radiusKm, Pageable pageable) {
        if (userLat != null && userLng != null) {
            // Fetch all matching cooks, calculate distance in memory, then paginate
            List<Cook> all = (city != null && !city.isBlank())
                    ? cookRepository.findAllByStatusesAndCity(VISIBLE_STATUSES, city)
                    : cookRepository.findAllByStatuses(VISIBLE_STATUSES);

            // Cooks with known coordinates — compute distance, optionally filter by radius
            List<CookSummaryResponse> withDist = all.stream()
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

            int start = (int) pageable.getOffset();
            int end   = Math.min(start + pageable.getPageSize(), withDist.size());
            List<CookSummaryResponse> page = start >= withDist.size()
                    ? new ArrayList<>() : withDist.subList(start, end);
            return new PageImpl<>(page, pageable, withDist.size());
        }

        // No location — sort by rating as before
        if (city != null && !city.isBlank()) {
            return cookRepository.findByStatusesAndCity(VISIBLE_STATUSES, city, pageable)
                    .map(c -> toSummary(c, null));
        }
        return cookRepository.findByStatuses(VISIBLE_STATUSES, pageable)
                .map(c -> toSummary(c, null));
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
        if (request.getWelcomeMessage() != null) cook.setWelcomeMessage(request.getWelcomeMessage());
        if (request.getDateOfBirth() != null) cook.setDateOfBirth(request.getDateOfBirth());

        return toDetail(cookRepository.save(cook));
    }

    public List<String> getAvailableCities() {
        return cookRepository.findDistinctActiveCities();
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
                .totalOrders(cook.getTotalOrders())
                .distanceKm(rounded)
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
                .menuItems(menuItems)
                .welcomeMessage(cook.getWelcomeMessage())
                .totalOrders(cook.getTotalOrders())
                .dateOfBirth(cook.getDateOfBirth())
                .reviewSummary(cook.getReviewSummary())
                .build();
    }
}
