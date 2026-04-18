package com.kloudshef.backend.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String city;
    private String profileImageUrl;
    private Boolean dpPublic;
    private String currentPassword;
    private String newPassword;
}
