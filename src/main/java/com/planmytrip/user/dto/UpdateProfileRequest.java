package com.planmytrip.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Please provide a valid phone number")
    private String phone;
}
