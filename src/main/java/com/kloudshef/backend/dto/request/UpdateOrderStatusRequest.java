package com.kloudshef.backend.dto.request;

import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    private String status;
    /// Minutes from now — cook sets this when accepting (e.g. 30 means "ready in 30 min")
    private Integer estimatedPickupMinutes;
}
