package com.planmytrip.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UserResponse user;

    public static AuthResponse of(String token, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(user)
                .build();
    }
}
