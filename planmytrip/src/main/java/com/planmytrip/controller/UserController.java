package com.planmytrip.controller;

import com.planmytrip.entity.User;
import com.planmytrip.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Register
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    // Login
    @PostMapping("/login")
    public User loginUser(@RequestBody User user) {
        return userService.loginUser(user.getEmail(), user.getPassword());
    }

    // Get All Users
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // Delete by ID
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "User deleted successfully";
    }

    // Get by ID
    @GetMapping("/{id}")
    public Object getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // Send OTP
    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String email) {
        userService.sendOtp(email);
        return "OTP sent successfully";
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public boolean verifyOtp(@RequestParam String email,
                             @RequestParam String otp) {
        return userService.verifyOtp(email, otp);
    }

    // Reset Password
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword) {
        userService.resetPassword(email, newPassword);
        return "Password reset successful";
    }
}