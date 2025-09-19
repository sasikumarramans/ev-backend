package com.evbooking.backend.usecase.service;

import com.evbooking.backend.domain.model.*;
import com.evbooking.backend.domain.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final OtpRequestRepository otpRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtTokenService jwtTokenService;

    public AuthenticationService(UserRepository userRepository,
                               OtpRequestRepository otpRequestRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               OtpService otpService,
                               JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.otpRequestRepository = otpRequestRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpService = otpService;
        this.jwtTokenService = jwtTokenService;
    }

    public OtpRequest sendOtp(String mobileNumber) {
        validateMobileNumber(mobileNumber);
        checkOtpRateLimit(mobileNumber);

        // Generate OTP (static 1234 for now)
        String otpCode = "1234";

        // Invalidate any existing pending OTPs
        invalidateExistingOtps(mobileNumber);

        // Create new OTP request
        OtpRequest otpRequest = new OtpRequest(mobileNumber, otpCode, OtpRequest.OtpType.LOGIN);
        otpRequest = otpRequestRepository.save(otpRequest);

        // Send OTP (for now just log it)
        otpService.sendOtp(mobileNumber, otpCode);

        return otpRequest;
    }

    public OtpRequest resendOtp(String mobileNumber) {
        validateMobileNumber(mobileNumber);
        checkOtpRateLimit(mobileNumber);

        // Find existing OTP request
        Optional<OtpRequest> existingOtp = otpRequestRepository.findLatestByMobileNumber(mobileNumber);

        if (existingOtp.isPresent() && !existingOtp.get().isExpired()) {
            // Reuse existing OTP
            OtpRequest otpRequest = existingOtp.get();
            otpService.sendOtp(mobileNumber, otpRequest.getOtpCode());
            return otpRequest;
        } else {
            // Create new OTP
            return sendOtp(mobileNumber);
        }
    }

    public AuthenticationResult verifyOtpAndLogin(String mobileNumber, String otpCode) {
        validateMobileNumber(mobileNumber);

        Optional<OtpRequest> otpRequestOpt = otpRequestRepository.findByMobileNumberAndStatus(
            mobileNumber, OtpRequest.OtpStatus.PENDING);

        if (otpRequestOpt.isEmpty()) {
            throw new RuntimeException("No pending OTP found for this mobile number");
        }

        OtpRequest otpRequest = otpRequestOpt.get();

        if (otpRequest.isExpired()) {
            otpRequest.setStatus(OtpRequest.OtpStatus.EXPIRED);
            otpRequestRepository.save(otpRequest);
            throw new RuntimeException("OTP has expired");
        }

        if (!otpRequest.getOtpCode().equals(otpCode)) {
            otpRequest.incrementAttempt();
            otpRequestRepository.save(otpRequest);
            throw new RuntimeException("Invalid OTP");
        }

        // Mark OTP as verified
        otpRequest.markAsVerified();
        otpRequestRepository.save(otpRequest);

        // Get or create user
        User user = getOrCreateUser(mobileNumber);

        // Generate tokens
        String accessToken = jwtTokenService.generateAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken(user);

        // Save refresh token
        saveRefreshToken(refreshToken, user.getId());

        return new AuthenticationResult(user, accessToken, refreshToken);
    }

    public AuthenticationResult refreshAccessToken(String refreshTokenStr) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(refreshTokenStr);

        if (refreshTokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (!refreshToken.isValid()) {
            refreshTokenRepository.deleteByToken(refreshTokenStr);
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        Optional<User> userOpt = userRepository.findById(refreshToken.getUserId());
        if (userOpt.isEmpty()) {
            refreshTokenRepository.deleteByToken(refreshTokenStr);
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Generate new access token
        String newAccessToken = jwtTokenService.generateAccessToken(user);

        return new AuthenticationResult(user, newAccessToken, refreshTokenStr);
    }

    public void logout(String refreshTokenStr) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(refreshTokenStr);
        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        }
    }

    public void logoutAllDevices(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void validateMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        // Basic mobile number validation (Indian format)
        String cleaned = mobileNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() != 10 && !(cleaned.length() == 13 && cleaned.startsWith("91"))) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }
    }

    private void checkOtpRateLimit(String mobileNumber) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int recentOtpCount = otpRequestRepository.countByMobileNumberAndCreatedAtAfter(mobileNumber, oneHourAgo);

        if (recentOtpCount >= 5) {
            throw new RuntimeException("Too many OTP requests. Please try again later");
        }
    }

    private void invalidateExistingOtps(String mobileNumber) {
        Optional<OtpRequest> existingOtp = otpRequestRepository.findByMobileNumberAndStatus(
            mobileNumber, OtpRequest.OtpStatus.PENDING);

        if (existingOtp.isPresent()) {
            OtpRequest otp = existingOtp.get();
            otp.setStatus(OtpRequest.OtpStatus.EXPIRED);
            otpRequestRepository.save(otp);
        }
    }

    private User getOrCreateUser(String mobileNumber) {
        Optional<User> existingUser = userRepository.findByPhoneNumber(mobileNumber);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setPhoneNumber(mobileNumber);
        newUser.setRole(UserRole.CUSTOMER);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    private void saveRefreshToken(String token, Long userId) {
        // Delete existing refresh tokens for user (single device login)
        refreshTokenRepository.deleteByUserId(userId);

        // Create new refresh token
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30); // 30 days
        RefreshToken refreshToken = new RefreshToken(token, userId, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }

    public static class AuthenticationResult {
        private final User user;
        private final String accessToken;
        private final String refreshToken;

        public AuthenticationResult(User user, String accessToken, String refreshToken) {
            this.user = user;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public User getUser() { return user; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}