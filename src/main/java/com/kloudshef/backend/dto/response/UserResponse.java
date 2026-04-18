package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String city;
    private String profileImageUrl;
    private boolean dpPublic;
    private String role;
    private Long cookId;
}
