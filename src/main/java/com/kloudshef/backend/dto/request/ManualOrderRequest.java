package com.kloudshef.backend.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ManualOrderRequest {
    private String customerName;
    private List<Map<String, Object>> items;
    private String deliveryNote;
}
