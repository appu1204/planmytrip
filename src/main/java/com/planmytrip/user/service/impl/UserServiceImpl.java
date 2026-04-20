package com.planmytrip.user.service.impl;

import com.planmytrip.common.exception.BadRequestException;
import com.planmytrip.common.exception.ResourceNotFoundException;
import com.planmytrip.user.dto.*;
import com.planmytrip.user.entity.User;
import com.planmytrip.user.mapper.UserMapper;
import com.planmytrip.user.repository.UserRepository;
import com.planmytrip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    // ─────────────────────────────────────────────
    // GET PROFILE
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserResponse> getProfile(String email) {
        User user = findUserByEmail(email);
        return ApiResponse.success("Profile fetched successfully", userMapper.toUserResponse(user));
    }

    // ─────────────────────────────────────────────
    // UPDATE PROFILE
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<UserResponse> updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());

        User updatedUser = userRepository.save(user);

        log.info("Profile updated for user: {}", email);

        return ApiResponse.success("Profile updated successfully", userMapper.toUserResponse(updatedUser));
    }

    // ─────────────────────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from your current password");
        }

        User user = findUserByEmail(email);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);

        return ApiResponse.success("Password changed successfully");
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
