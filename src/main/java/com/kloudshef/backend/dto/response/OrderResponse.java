package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long cookId;
    private String cookKitchenName;
    private List<OrderItemResponse> items;
    private String status;
    private BigDecimal totalAmount;
    private String deliveryNote;
    private LocalDateTime estimatedPickupTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isManual;
    private String cookAddress;
    private Double cookLatitude;
    private Double cookLongitude;
    private String cookCountry;
}
