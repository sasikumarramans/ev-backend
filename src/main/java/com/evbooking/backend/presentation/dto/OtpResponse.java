package com.evbooking.backend.presentation.dto;

import java.time.LocalDateTime;

public class OtpResponse {
    private Long otpId;
    private String message;
    private LocalDateTime expiresAt;

    public OtpResponse() {}

    public OtpResponse(Long otpId, String message, LocalDateTime expiresAt) {
        this.otpId = otpId;
        this.message = message;
        this.expiresAt = expiresAt;
    }

    public Long getOtpId() {
        return otpId;
    }

    public void setOtpId(Long otpId) {
        this.otpId = otpId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}