package com.planmytrip.user.controller;

import com.planmytrip.user.dto.*;
import com.planmytrip.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        ApiResponse<UserResponse> response = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/users/me
     * Update the currently authenticated user's profile (name and phone).
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        ApiResponse<UserResponse> response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/users/me/change-password
     * Change the currently authenticated user's password.
     */
    @PutMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        ApiResponse<Void> response = userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
