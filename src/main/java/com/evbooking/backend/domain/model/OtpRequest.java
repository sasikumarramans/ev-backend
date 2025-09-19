package com.evbooking.backend.domain.model;

import java.time.LocalDateTime;

public class OtpRequest {
    private Long id;
    private String mobileNumber;
    private String otpCode;
    private OtpStatus status;
    private OtpType type;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;

    public OtpRequest() {}

    public OtpRequest(String mobileNumber, String otpCode, OtpType type) {
        this.mobileNumber = mobileNumber;
        this.otpCode = otpCode;
        this.type = type;
        this.status = OtpStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5); // 5 minutes expiry
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public OtpStatus getStatus() { return status; }
    public void setStatus(OtpStatus status) { this.status = status; }

    public OtpType getType() { return type; }
    public void setType(OtpType type) { this.type = type; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canRetry() {
        return attemptCount < 3 && !isExpired() && status == OtpStatus.PENDING;
    }

    public void incrementAttempt() {
        this.attemptCount++;
        if (this.attemptCount >= 3) {
            this.status = OtpStatus.FAILED;
        }
    }

    public void markAsVerified() {
        this.status = OtpStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public enum OtpStatus {
        PENDING,
        VERIFIED,
        EXPIRED,
        FAILED
    }

    public enum OtpType {
        LOGIN,
        REGISTRATION,
        PASSWORD_RESET
    }
}