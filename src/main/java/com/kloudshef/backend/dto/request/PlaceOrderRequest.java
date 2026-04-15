package com.kloudshef.backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {
    private Long cookId;
    private List<OrderItemRequest> items;
    private String deliveryNote;
}
