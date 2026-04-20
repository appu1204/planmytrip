package com.planmytrip.user.dto;

import com.planmytrip.user.enums.Provider;
import com.planmytrip.user.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Provider provider;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
