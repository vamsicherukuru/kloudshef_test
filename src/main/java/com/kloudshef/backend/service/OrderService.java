package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.PlaceOrderRequest;
import com.kloudshef.backend.dto.response.OrderItemResponse;
import com.kloudshef.backend.dto.response.OrderResponse;
import com.kloudshef.backend.entity.*;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.MenuItemRepository;
import com.kloudshef.backend.repository.OrderRepository;
import com.kloudshef.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CookRepository cookRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public List<OrderResponse> placeOrder(Long customerId, PlaceOrderRequest request) {
        Cook cook = cookRepository.findById(request.getCookId())
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found with id: " + request.getCookId()));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + customerId));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(itemRequest.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemRequest.getMenuItemId()));

            if (!menuItem.getCook().getId().equals(cook.getId())) {
                throw new BadRequestException("Menu item " + menuItem.getId() + " does not belong to the specified cook");
            }

            BigDecimal subtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .itemPrice(menuItem.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .subtotal(subtotal)
                    .isVegetarian(menuItem.isVegetarian())
                    .build();

            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .customer(customer)
                .cook(cook)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .deliveryNote(request.getDeliveryNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        savedOrder.getItems().addAll(orderItems);
        orderRepository.save(savedOrder);

        // Notify cook about new order
        String cookFcmToken = cook.getUser() != null ? cook.getUser().getFcmToken() : null;
        fcmService.sendNotification(
                cookFcmToken,
                "New Order!",
                customer.getFullName() + " just placed an order from " + cook.getKitchenName()
        );

        return getMyOrders(customerId);
    }

    public List<OrderResponse> getMyOrders(Long customerId) {
        return orderRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getCookOrders(Long cookId) {
        return orderRepository.findByCook_IdOrderByCreatedAtDesc(cookId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Long cookId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCook().getId().equals(cookId)) {
            throw new BadRequestException("You are not authorized to update this order");
        }

        OrderStatus parsed = OrderStatus.valueOf(newStatus);
        OrderStatus previous = order.getStatus();
        order.setStatus(parsed);
        Order saved = orderRepository.save(order);

        // Increment cook's completed orders count when an order reaches COMPLETED
        if (parsed == OrderStatus.COMPLETED && previous != OrderStatus.COMPLETED) {
            Cook cook = saved.getCook();
            cook.setTotalOrders(cook.getTotalOrders() + 1);
            cookRepository.save(cook);
        }

        // Notify customer about status change
        String customerToken = saved.getCustomer().getFcmToken();
        String statusMsg = switch (parsed) {
            case CONFIRMED  -> "Your order from " + saved.getCook().getKitchenName() + " is confirmed!";
            case READY      -> "Your order from " + saved.getCook().getKitchenName() + " is ready for pickup!";
            case COMPLETED  -> "Order from " + saved.getCook().getKitchenName() + " completed. Enjoy!";
            case CANCELLED  -> "Your order from " + saved.getCook().getKitchenName() + " was cancelled.";
            default         -> null;
        };
        if (statusMsg != null) {
            fcmService.sendNotification(customerToken, "Order Update", statusMsg);
        }

        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .menuItemId(item.getMenuItemId())
                        .itemName(item.getItemName())
                        .itemPrice(item.getItemPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .isVegetarian(item.isVegetarian())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .cookId(order.getCook().getId())
                .cookKitchenName(order.getCook().getKitchenName())
                .cookAddress(order.getCook().getAddress())
                .cookLatitude(order.getCook().getLatitude())
                .cookLongitude(order.getCook().getLongitude())
                .cookCountry(order.getCook().getCountry())
                .items(itemResponses)
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .deliveryNote(order.getDeliveryNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
