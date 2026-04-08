package com.planmytrip.service;

import com.planmytrip.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    // Register
    User registerUser(User user);

    // Login
    User loginUser(String email, String password);

    // Update
    User updateUser(User user);

    // Delete
    void deleteUser(Long id);

    // Get All Users
    List<User> getAllUsers();

    // Get by ID
    Optional<User> getUserById(Long id);

    // Get by Email
    Optional<User> getUserByEmail(String email);

    // Forgot Password - Step 1: Send OTP
    void sendOtp(String email);

    // Forgot Password - Step 2: Verify OTP
    boolean verifyOtp(String email, String otp);

    // Forgot Password - Step 3: Reset Password
    void resetPassword(String email, String newPassword);
}