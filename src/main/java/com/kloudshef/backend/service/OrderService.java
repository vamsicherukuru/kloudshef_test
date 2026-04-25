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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CookRepository cookRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    private static final int MAX_ACTIVE_ORDERS = 4;

    @Transactional
    public List<OrderResponse> placeOrder(Long customerId, PlaceOrderRequest request) {
        // Rate limit: max 4 active orders per customer
        long activeCount = orderRepository.countByCustomer_IdAndStatusIn(
                customerId,
                List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED,
                        OrderStatus.PREPARING, OrderStatus.PACKING,
                        OrderStatus.READY_TO_PICKUP));
        if (activeCount >= MAX_ACTIVE_ORDERS) {
            throw new BadRequestException(
                    "You can have at most " + MAX_ACTIVE_ORDERS + " active orders at a time. " +
                    "Please wait for a current order to complete before placing a new one.");
        }

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

        // Notify cook on ALL devices
        int itemCount = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
        String itemWord = itemCount == 1 ? "item" : "items";
        if (cook.getUser() != null) {
            fcmService.sendToUser(
                    cook.getUser().getId(),
                    "New Pickup Request!",
                    customer.getFullName() + " ordered " + itemCount + " " + itemWord
                            + " worth " + totalAmount.stripTrailingZeros().toPlainString(),
                    "new_order"
            );
        }

        return getMyOrders(customerId);
    }

    @Transactional
    public OrderResponse createManualOrder(Long cookId, String customerName,
            List<Map<String, Object>> items, String deliveryNote) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found with id: " + cookId));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var item : items) {
            String name = (String) item.get("name");
            int qty = ((Number) item.get("quantity")).intValue();
            double price = item.get("price") != null ? ((Number) item.get("price")).doubleValue() : 0.0;
            BigDecimal unitPrice = BigDecimal.valueOf(price);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(subtotal);

            Long menuItemId = item.get("menuItemId") != null
                    ? ((Number) item.get("menuItemId")).longValue() : null;

            orderItems.add(OrderItem.builder()
                    .menuItemId(menuItemId)
                    .itemName(name)
                    .itemPrice(unitPrice)
                    .quantity(qty)
                    .subtotal(subtotal)
                    .isVegetarian(false)
                    .build());
        }

        // Use the cook's own user as the "customer" placeholder but store name override
        User cookUser = cook.getUser();

        Order order = Order.builder()
                .customer(cookUser)
                .cook(cook)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .deliveryNote(deliveryNote)
                .isManual(true)
                .build();

        // Stamp the customer name via deliveryNote prefix if no real user
        // Actually store it as a special deliveryNote token so the response carries it
        // We handle customerName by patching the response after save
        Order savedOrder = orderRepository.save(order);
        for (OrderItem oi : orderItems) {
            oi.setOrder(savedOrder);
        }
        savedOrder.getItems().addAll(orderItems);
        savedOrder = orderRepository.save(savedOrder);

        OrderResponse resp = toResponse(savedOrder);
        // Override customer name with the provided walk-in name
        return OrderResponse.builder()
                .id(resp.getId())
                .customerId(resp.getCustomerId())
                .customerName(customerName)
                .cookId(resp.getCookId())
                .cookKitchenName(resp.getCookKitchenName())
                .cookAddress(resp.getCookAddress())
                .cookLatitude(resp.getCookLatitude())
                .cookLongitude(resp.getCookLongitude())
                .cookCountry(resp.getCookCountry())
                .items(resp.getItems())
                .status(resp.getStatus())
                .totalAmount(resp.getTotalAmount())
                .deliveryNote(deliveryNote)
                .estimatedPickupTime(resp.getEstimatedPickupTime())
                .createdAt(resp.getCreatedAt())
                .updatedAt(resp.getUpdatedAt())
                .isManual(true)
                .build();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Cannot cancel — order is already being prepared. Please contact the cook directly.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Notify the cook
        String customerName = saved.getCustomer().getFullName();
        fcmService.sendToUser(
                saved.getCook().getUser().getId(),
                "Order Cancelled",
                customerName + " cancelled their order #" + orderId,
                "order_update"
        );

        return toResponse(saved);
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
    public OrderResponse updateOrderStatus(Long orderId, Long cookId, String newStatus, Integer estimatedPickupMinutes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCook().getId().equals(cookId)) {
            throw new BadRequestException("You are not authorized to update this order");
        }

        OrderStatus parsed = OrderStatus.valueOf(newStatus);
        OrderStatus previous = order.getStatus();
        order.setStatus(parsed);

        // Set estimated pickup time when cook accepts (or updates it)
        if (estimatedPickupMinutes != null && estimatedPickupMinutes > 0) {
            order.setEstimatedPickupTime(LocalDateTime.now().plusMinutes(estimatedPickupMinutes));
        }

        Order saved = orderRepository.save(order);

        // Increment cook's completed orders count when an order reaches COMPLETED
        if (parsed == OrderStatus.COMPLETED && previous != OrderStatus.COMPLETED) {
            Cook cook = saved.getCook();
            cook.setTotalOrders(cook.getTotalOrders() + 1);
            cookRepository.save(cook);
        }

        // Notify customer on ALL devices
        String kitchen = saved.getCook().getKitchenName();
        record NotifContent(String title, String body) {}
        NotifContent notif = switch (parsed) {
            case ACCEPTED       -> new NotifContent(
                    "Order Accepted! ✅",
                    kitchen + " accepted your order."
                            + (saved.getEstimatedPickupTime() != null
                                ? " Estimated pickup: ~" + estimatedPickupMinutes + " min."
                                : ""));
            case PREPARING      -> new NotifContent(
                    "Being Prepared",
                    kitchen + " is cooking your order right now. Hang tight!");
            case PACKING        -> new NotifContent(
                    "Almost Ready!",
                    kitchen + " is packing your order. Won't be long now!");
            case READY_TO_PICKUP -> new NotifContent(
                    "Ready for Pickup!",
                    "Your order from " + kitchen + " is ready. Head over now!");
            case COMPLETED      -> new NotifContent(
                    "Order Complete!",
                    "Enjoy your meal from " + kitchen + "! Leave a review if you loved it.");
            case CANCELLED      -> new NotifContent(
                    "Order Cancelled",
                    "Your order from " + kitchen + " was cancelled.");
            default             -> null;
        };
        if (notif != null) {
            var extra = new java.util.HashMap<String, String>();
            extra.put("orderId", saved.getId().toString());
            extra.put("status", parsed.name());
            if (saved.getEstimatedPickupTime() != null) {
                extra.put("estimatedPickupTime", saved.getEstimatedPickupTime().toString());
            }
            fcmService.sendToUser(saved.getCustomer().getId(), notif.title(), notif.body(), "order_update", extra);
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
                .estimatedPickupTime(order.getEstimatedPickupTime())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .isManual(order.isManual())
                .build();
    }
}
