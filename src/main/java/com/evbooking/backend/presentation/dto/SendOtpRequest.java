package com.evbooking.backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$|^91[0-9]{10}$",
             message = "Mobile number must be 10 digits or 13 digits starting with 91")
    private String mobileNumber;

    public SendOtpRequest() {}

    public SendOtpRequest(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }
}