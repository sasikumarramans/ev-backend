package com.evbooking.backend.usecase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    public void sendOtp(String mobileNumber, String otpCode) {
        // For now, just log the OTP. In production, integrate with SMS service
        logger.info("Sending OTP to mobile number: {} | OTP: {}",
                   maskMobileNumber(mobileNumber), otpCode);

        // TODO: Integrate with SMS service like Twilio, AWS SNS, or local SMS provider
        // Example integration:
        // smsProvider.sendSms(mobileNumber, "Your OTP is: " + otpCode + ". Valid for 5 minutes.");

        simulateSmsSending();
    }

    private void simulateSmsSending() {
        try {
            // Simulate network delay
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("SMS sending simulation interrupted");
        }
    }

    private String maskMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.length() < 4) {
            return "****";
        }

        String cleaned = mobileNumber.replaceAll("[^0-9]", "");
        int length = cleaned.length();

        if (length <= 4) {
            return "****";
        }

        String masked = cleaned.substring(0, 2) + "****" + cleaned.substring(length - 2);
        return masked;
    }

    public boolean validateOtpFormat(String otp) {
        return otp != null && otp.matches("\\d{4}");
    }
}