package com.planmytrip.user.service.impl;

import com.planmytrip.common.exception.AccountLockedException;
import com.planmytrip.common.exception.BadRequestException;
import com.planmytrip.common.exception.ResourceNotFoundException;
import com.planmytrip.common.exception.TokenExpiredException;
import com.planmytrip.common.security.JwtUtil;
import com.planmytrip.user.dto.*;
import com.planmytrip.user.entity.EmailVerificationToken;
import com.planmytrip.user.entity.LoginOtpToken;
import com.planmytrip.user.entity.PasswordResetToken;
import com.planmytrip.user.entity.User;
import com.planmytrip.user.enums.Provider;
import com.planmytrip.user.enums.Role;
import com.planmytrip.user.mapper.UserMapper;
import com.planmytrip.user.repository.EmailVerificationTokenRepository;
import com.planmytrip.user.repository.LoginOtpTokenRepository;
import com.planmytrip.user.repository.PasswordResetTokenRepository;
import com.planmytrip.user.repository.UserRepository;
import com.planmytrip.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final LoginOtpTokenRepository loginOtpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${app.token.password-reset-expiry-minutes}")
    private int passwordResetExpiryMinutes;

    @Value("${app.token.email-verification-expiry-hours}")
    private int emailVerificationExpiryHours;

    @Value("${app.token.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .isVerified(false)
                .isActive(true)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);

        sendEmailVerification(user);

        log.info("New user registered: {}", user.getEmail());

        return ApiResponse.success(
                "Registration successful. Please check your email to verify your account."
        );
    }

    // ─────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new BadRequestException("Your account has been deactivated. Please contact support.");
        }

        if (user.isAccountLocked()) {
            throw new AccountLockedException(
                    "Your account is temporarily locked due to multiple failed login attempts. " +
                    "Please try again after " + user.getLockUntil()
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLoginAttempt(user);
            throw new BadRequestException("Invalid email or password");
        }

        // Reset failed attempts on successful password match
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        // Generate and send OTP
        String otp = generateOtp();
        saveLoginOtp(user, otp);
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Login OTP sent to: {}", user.getEmail());

        return ApiResponse.success(
                "OTP has been sent to your registered email address. Please verify to continue."
        );
    }

    // ─────────────────────────────────────────────
    // VERIFY OTP
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        LoginOtpToken otpToken = loginOtpTokenRepository
                .findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BadRequestException("No active OTP found. Please login again."));

        if (otpToken.isExpired()) {
            throw new TokenExpiredException("OTP has expired. Please login again to receive a new OTP.");
        }

        if (!otpToken.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        // Mark OTP as used
        otpToken.setUsed(true);
        loginOtpTokenRepository.save(otpToken);

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String jwtToken = jwtUtil.generateToken(userDetails);

        UserResponse userResponse = userMapper.toUserResponse(user);
        AuthResponse authResponse = AuthResponse.of(jwtToken, userResponse);

        log.info("User logged in successfully: {}", user.getEmail());

        return ApiResponse.success("Login successful", authResponse);
    }

    // ─────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + request.getEmail()
                ));

        // Invalidate any previous unused tokens
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryTime(LocalDateTime.now().plusMinutes(passwordResetExpiryMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);

        log.info("Password reset link sent to: {}", user.getEmail());

        return ApiResponse.success(
                "Password reset link has been sent to your email address. Link expires in "
                + passwordResetExpiryMinutes + " minutes."
        );
    }

    // ─────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or non-existent reset token"));

        if (resetToken.getUsed()) {
            throw new BadRequestException("This reset link has already been used");
        }

        if (resetToken.isExpired()) {
            throw new TokenExpiredException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());

        return ApiResponse.success("Your password has been reset successfully. You can now login with your new password.");
    }

    // ─────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse<Void> verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or non-existent verification token"));

        if (verificationToken.getUsed()) {
            throw new BadRequestException("This verification link has already been used");
        }

        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("This verification link has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();

        if (user.getIsVerified()) {
            throw new BadRequestException("Your email is already verified");
        }

        user.setIsVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified successfully for user: {}", user.getEmail());

        return ApiResponse.success("Your email has been verified successfully. You can now login.");
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private void handleFailedLoginAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            log.warn("Account locked for user: {} after {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
    }

    private void sendEmailVerification(User user) {
        emailVerificationTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiryTime(LocalDateTime.now().plusHours(emailVerificationExpiryHours))
                .used(false)
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }

    private void saveLoginOtp(User user, String otp) {
        loginOtpTokenRepository.deleteAllByUserId(user.getId());

        LoginOtpToken otpToken = LoginOtpToken.builder()
                .user(user)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();

        loginOtpTokenRepository.save(otpToken);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // ─────────────────────────────────────────────
    // EMAIL SENDING
    // ─────────────────────────────────────────────

    private void sendVerificationEmail(String email, String name, String token) {
        String verificationLink = appBaseUrl + "/auth/verify-email?token=" + token;
        String subject = "Verify Your Email Address";
        String body = "Hi " + name + ",\n\n"
                + "Please click the link below to verify your email:\n"
                + verificationLink + "\n\n"
                + "This link expires in " + emailVerificationExpiryHours + " hours.\n\n"
                + "PlanMyTrip Team";
        sendEmail(email, subject, body);
    }

    private void sendOtpEmail(String email, String name, String otp) {
        String subject = "Your Login OTP";
        String body = "Hi " + name + ",\n\n"
                + "Your one-time login OTP is: " + otp + "\n\n"
                + "This OTP expires in " + otpExpiryMinutes + " minutes.\n"
                + "Do not share it with anyone.\n\n"
                + "PlanMyTrip Team";
        sendEmail(email, subject, body);
    }

    private void sendPasswordResetEmail(String email, String name, String token) {
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        String subject = "Reset Your Password";
        String body = "Hi " + name + ",\n\n"
                + "Click the link below to reset your password:\n"
                + resetLink + "\n\n"
                + "This link expires in " + passwordResetExpiryMinutes + " minutes.\n"
                + "If you did not request this, ignore this email.\n\n"
                + "PlanMyTrip Team";
        sendEmail(email, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {} with subject {}", to, subject, ex);
            throw new RuntimeException("Unable to send email at the moment. Please try again later.");
        }
    }
}
