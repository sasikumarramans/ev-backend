package com.evbooking.backend.infrastructure.repository;

import com.evbooking.backend.domain.model.OtpRequest;
import com.evbooking.backend.infrastructure.entity.OtpRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface JpaOtpRequestRepository extends JpaRepository<OtpRequestEntity, Long> {

    Optional<OtpRequestEntity> findByMobileNumberAndStatus(String mobileNumber, OtpRequest.OtpStatus status);

    @Query("SELECT o FROM OtpRequestEntity o WHERE o.mobileNumber = :mobileNumber ORDER BY o.createdAt DESC")
    Optional<OtpRequestEntity> findLatestByMobileNumber(@Param("mobileNumber") String mobileNumber);

    void deleteByExpiresAtBefore(LocalDateTime before);

    int countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);
}