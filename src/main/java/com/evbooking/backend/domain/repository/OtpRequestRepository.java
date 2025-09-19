package com.evbooking.backend.domain.repository;

import com.evbooking.backend.domain.model.OtpRequest;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRequestRepository {
    Optional<OtpRequest> findById(Long id);
    Optional<OtpRequest> findByMobileNumberAndStatus(String mobileNumber, OtpRequest.OtpStatus status);
    Optional<OtpRequest> findLatestByMobileNumber(String mobileNumber);
    OtpRequest save(OtpRequest otpRequest);
    void deleteById(Long id);
    void deleteExpiredOtps(LocalDateTime before);
    int countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);
}