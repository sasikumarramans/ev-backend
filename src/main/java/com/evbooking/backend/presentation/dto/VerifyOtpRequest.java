package com.evbooking.backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$|^91[0-9]{10}$",
             message = "Mobile number must be 10 digits or 13 digits starting with 91")
    private String mobileNumber;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "OTP must be 4 digits")
    private String otpCode;

    public VerifyOtpRequest() {}

    public VerifyOtpRequest(String mobileNumber, String otpCode) {
        this.mobileNumber = mobileNumber;
        this.otpCode = otpCode;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}