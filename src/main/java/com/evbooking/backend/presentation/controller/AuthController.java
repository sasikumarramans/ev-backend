package com.evbooking.backend.presentation.controller;

import com.evbooking.backend.presentation.dto.*;
import com.evbooking.backend.usecase.service.AuthenticationService;
import com.evbooking.backend.usecase.service.JwtTokenService;
import com.evbooking.backend.domain.model.OtpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthenticationService authenticationService,
                         JwtTokenService jwtTokenService) {
        this.authenticationService = authenticationService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            OtpRequest otpRequest = authenticationService.sendOtp(request.getMobileNumber());

            OtpResponse response = new OtpResponse(
                otpRequest.getId(),
                "OTP sent successfully",
                otpRequest.getExpiresAt()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            OtpRequest otpRequest = authenticationService.resendOtp(request.getMobileNumber());

            OtpResponse response = new OtpResponse(
                otpRequest.getId(),
                "OTP resent successfully",
                otpRequest.getExpiresAt()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            AuthenticationService.AuthenticationResult result = authenticationService
                .verifyOtpAndLogin(request.getMobileNumber(), request.getOtpCode());

            AuthenticationResponse response = new AuthenticationResponse(
                result.getUser().getId(),
                result.getUser().getPhoneNumber(),
                result.getUser().getEmail(),
                result.getUser().getFirstName(),
                result.getUser().getLastName(),
                result.getUser().getRole().name(),
                result.getAccessToken(),
                result.getRefreshToken()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthenticationService.AuthenticationResult result = authenticationService
                .refreshAccessToken(request.getRefreshToken());

            RefreshTokenResponse response = new RefreshTokenResponse(
                result.getAccessToken(),
                result.getRefreshToken()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            authenticationService.logout(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutAllDevices(HttpServletRequest request) {
        try {
            // Get user ID from request attributes (set by JWT filter)
            Long userId = (Long) request.getAttribute("userId");

            if (userId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not authenticated"));
            }

            authenticationService.logoutAllDevices(userId);
            return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/generate-client-token")
    public ResponseEntity<ApiResponse<ClientTokenResponse>> generateClientToken() {
        // This endpoint should be protected and only accessible during development
        // In production, client tokens should be pre-generated and distributed
        try {
            String clientToken = jwtTokenService.generateClientApiToken();

            ClientTokenResponse response = new ClientTokenResponse(
                clientToken,
                "Client API token generated successfully. Use this in Authorization header with Bearer prefix."
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}