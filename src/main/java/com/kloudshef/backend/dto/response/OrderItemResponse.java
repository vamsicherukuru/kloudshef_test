package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long menuItemId;
    private String itemName;
    private BigDecimal itemPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private boolean isVegetarian;
}
