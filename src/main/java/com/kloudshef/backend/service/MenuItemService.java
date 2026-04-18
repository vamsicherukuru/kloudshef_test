package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.MenuItemRequest;
import com.kloudshef.backend.dto.response.MenuItemResponse;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.MenuItem;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.FollowRepository;
import com.kloudshef.backend.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CookRepository cookRepository;
    private final FollowRepository followRepository;
    private final FcmService fcmService;

    public List<MenuItemResponse> getMenuItemsByCookId(Long cookId) {
        return menuItemRepository.findByCookId(cookId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long userId, MenuItemRequest request) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));
        MenuItem item = MenuItem.builder()
                .cook(cook)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .tags(request.getTags())
                .available(request.isAvailable())
                .vegetarian(request.isVegetarian())
                .build();
        MenuItemResponse response = toResponse(menuItemRepository.save(item));

        // Notify followers about the new dish
        List<Long> followerIds = followRepository.findFollowerUserIdsByCookId(cook.getId());
        if (!followerIds.isEmpty()) {
            String title = "🍽 New dish from " + cook.getKitchenName() + "!";
            String body = request.getName() + (request.getDescription() != null ? " — " + request.getDescription() : "");
            Map<String, String> data = Map.of("cookId", cook.getId().toString());
            for (Long followerId : followerIds) {
                fcmService.sendToUser(followerId, title, body, "new_dish", data);
            }
        }

        return response;
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long userId, Long itemId, MenuItemRequest request) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        if (!item.getCook().getId().equals(cook.getId())) {
            throw new BadRequestException("You don't own this menu item");
        }
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        item.setImageUrl(request.getImageUrl());
        item.setTags(request.getTags());
        item.setAvailable(request.isAvailable());
        item.setVegetarian(request.isVegetarian());
        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void deleteMenuItem(Long userId, Long itemId) {
        Cook cook = cookRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook profile not found"));
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        if (!item.getCook().getId().equals(cook.getId())) {
            throw new BadRequestException("You don't own this menu item");
        }
        menuItemRepository.delete(item);
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
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
                .build();
    }
}
