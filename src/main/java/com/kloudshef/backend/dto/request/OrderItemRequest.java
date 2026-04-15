package com.kloudshef.backend.dto.request;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long menuItemId;
    private Integer quantity;
}
