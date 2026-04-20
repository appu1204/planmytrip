package com.planmytrip.user.service;

import com.planmytrip.user.dto.*;

public interface AuthService {

    ApiResponse<Void> register(RegisterRequest request);

    ApiResponse<Void> login(LoginRequest request);

    ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request);

    ApiResponse<Void> forgotPassword(ForgotPasswordRequest request);

    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

    ApiResponse<Void> verifyEmail(String token);
}
