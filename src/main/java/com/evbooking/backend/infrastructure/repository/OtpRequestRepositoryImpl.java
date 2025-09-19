package com.evbooking.backend.infrastructure.repository;

import com.evbooking.backend.domain.model.OtpRequest;
import com.evbooking.backend.domain.repository.OtpRequestRepository;
import com.evbooking.backend.infrastructure.entity.OtpRequestEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class OtpRequestRepositoryImpl implements OtpRequestRepository {

    private final JpaOtpRequestRepository jpaOtpRequestRepository;

    public OtpRequestRepositoryImpl(JpaOtpRequestRepository jpaOtpRequestRepository) {
        this.jpaOtpRequestRepository = jpaOtpRequestRepository;
    }

    @Override
    public Optional<OtpRequest> findById(Long id) {
        return jpaOtpRequestRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<OtpRequest> findByMobileNumberAndStatus(String mobileNumber, OtpRequest.OtpStatus status) {
        return jpaOtpRequestRepository.findByMobileNumberAndStatus(mobileNumber, status).map(this::toDomain);
    }

    @Override
    public Optional<OtpRequest> findLatestByMobileNumber(String mobileNumber) {
        return jpaOtpRequestRepository.findLatestByMobileNumber(mobileNumber).map(this::toDomain);
    }

    @Override
    public OtpRequest save(OtpRequest otpRequest) {
        OtpRequestEntity entity = toEntity(otpRequest);
        OtpRequestEntity saved = jpaOtpRequestRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaOtpRequestRepository.deleteById(id);
    }

    @Override
    public void deleteExpiredOtps(LocalDateTime before) {
        jpaOtpRequestRepository.deleteByExpiresAtBefore(before);
    }

    @Override
    public int countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after) {
        return jpaOtpRequestRepository.countByMobileNumberAndCreatedAtAfter(mobileNumber, after);
    }

    private OtpRequest toDomain(OtpRequestEntity entity) {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setId(entity.getId());
        otpRequest.setMobileNumber(entity.getMobileNumber());
        otpRequest.setOtpCode(entity.getOtpCode());
        otpRequest.setStatus(entity.getStatus());
        otpRequest.setType(entity.getType());
        otpRequest.setAttemptCount(entity.getAttemptCount());
        otpRequest.setCreatedAt(entity.getCreatedAt());
        otpRequest.setExpiresAt(entity.getExpiresAt());
        otpRequest.setVerifiedAt(entity.getVerifiedAt());
        return otpRequest;
    }

    private OtpRequestEntity toEntity(OtpRequest otpRequest) {
        OtpRequestEntity entity = new OtpRequestEntity();
        entity.setId(otpRequest.getId());
        entity.setMobileNumber(otpRequest.getMobileNumber());
        entity.setOtpCode(otpRequest.getOtpCode());
        entity.setStatus(otpRequest.getStatus());
        entity.setType(otpRequest.getType());
        entity.setAttemptCount(otpRequest.getAttemptCount());
        entity.setCreatedAt(otpRequest.getCreatedAt());
        entity.setExpiresAt(otpRequest.getExpiresAt());
        entity.setVerifiedAt(otpRequest.getVerifiedAt());
        return entity;
    }
}