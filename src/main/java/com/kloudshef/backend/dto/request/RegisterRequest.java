package com.kloudshef.backend.dto.request;

import com.kloudshef.backend.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    private String city;

    private Role role = Role.CUSTOMER;

    // Cook-only fields
    private String kitchenName;
    private String kitchenHandle;
    private LocalDate dateOfBirth;

    @AssertTrue(message = "You must accept the Terms & Conditions")
    private boolean termsAccepted;
}
