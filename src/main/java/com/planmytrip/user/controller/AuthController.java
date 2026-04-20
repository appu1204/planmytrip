package com.planmytrip.user.controller;

import com.planmytrip.user.dto.*;
import com.planmytrip.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        ApiResponse<Void> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * Authenticate credentials and send OTP to email.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest request) {
        ApiResponse<Void> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/verify-otp
     * Verify OTP and return JWT token.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        ApiResponse<AuthResponse> response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/forgot-password
     * Send password reset link to email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ApiResponse<Void> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/reset-password
     * Reset password using the token from email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ApiResponse<Void> response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/verify-email?token=xxx
     * Verify user email using the token from email link.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        ApiResponse<Void> response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }
}
