package com.evbooking.backend.domain.repository;

import com.evbooking.backend.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findById(Long id);
    Optional<RefreshToken> findByToken(String token);
    RefreshToken save(RefreshToken refreshToken);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
    void deleteByToken(String token);
}