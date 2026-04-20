package com.planmytrip.user.service;

import com.planmytrip.user.dto.*;

public interface UserService {

    ApiResponse<UserResponse> getProfile(String email);

    ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request);

    ApiResponse<Void> changePassword(String email, ChangePasswordRequest request);
}
