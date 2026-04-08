package com.planmytrip.service;

import com.planmytrip.entity.User;
import com.planmytrip.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Temporary OTP storage (email → OTP)
    private Map<String, String> otpStorage = new HashMap<>();

    // Register User

    @Override
    public User registerUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        requireText(user.getName(), "name is required");
        requireText(user.getEmail(), "email is required");
        requireText(user.getPassword(), "password is required");
        requireText(user.getPhone(), "phone is required");

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        if (userRepository.existsByPhone(user.getPhone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }

        // Set required fields to satisfy non-null DB constraints
        user.setRole("USER");
        user.setIsVerified(false);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        LocalDateTime now = LocalDateTime.now();
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    // Login User

    @Override
    public User loginUser(String email, String password) {
        requireText(email, "email is required");
        requireText(password, "password is required");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        return user;
    }

    // Update User

    @Override
    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        Optional<User> existingUserOpt = userRepository.findById(user.getId());

        if (existingUserOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User existingUser = existingUserOpt.get();

        // Update only allowed fields
        existingUser.setName(user.getName());
        existingUser.setPhone(user.getPhone());
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        existingUser.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(existingUser);
    }

    // Delete User

    @Override
    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        userRepository.deleteById(id);
    }

    // Get All Users

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get User by ID

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get User by Email

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Step 1: Send OTP

    @Override
    public void sendOtp(String email) {
        requireText(email, "email is required");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email not registered");
        }

        //Generate 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        //Store OTP
        otpStorage.put(email, otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("PlanMyTrip OTP Verification");
            message.setText("Your OTP is: " + otp + ". It is valid for your current reset flow.");
            mailSender.send(message);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to send OTP email");
        }
    }

    // Step 2: Verify OTP

    @Override
    public boolean verifyOtp(String email, String otp) {

        String storedOtp = otpStorage.get(email);

        return storedOtp != null && storedOtp.equals(otp);
    }

    // Step 3: Reset Password

    @Override
    public void resetPassword(String email, String newPassword) {
        requireText(email, "email is required");
        requireText(newPassword, "newPassword is required");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        // Remove OTP after reset
        otpStorage.remove(email);
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

}