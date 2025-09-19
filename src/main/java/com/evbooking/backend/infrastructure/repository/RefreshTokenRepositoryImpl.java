package com.evbooking.backend.infrastructure.repository;

import com.evbooking.backend.domain.model.RefreshToken;
import com.evbooking.backend.domain.repository.RefreshTokenRepository;
import com.evbooking.backend.infrastructure.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;

    public RefreshTokenRepositoryImpl(JpaRefreshTokenRepository jpaRefreshTokenRepository) {
        this.jpaRefreshTokenRepository = jpaRefreshTokenRepository;
    }

    @Override
    public Optional<RefreshToken> findById(Long id) {
        return jpaRefreshTokenRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRefreshTokenRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = toEntity(refreshToken);
        RefreshTokenEntity saved = jpaRefreshTokenRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRefreshTokenRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRefreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteByToken(String token) {
        jpaRefreshTokenRepository.deleteByToken(token);
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(entity.getId());
        refreshToken.setToken(entity.getToken());
        refreshToken.setUserId(entity.getUserId());
        refreshToken.setExpiresAt(entity.getExpiresAt());
        refreshToken.setCreatedAt(entity.getCreatedAt());
        refreshToken.setRevoked(entity.isRevoked());
        return refreshToken;
    }

    private RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(refreshToken.getId());
        entity.setToken(refreshToken.getToken());
        entity.setUserId(refreshToken.getUserId());
        entity.setExpiresAt(refreshToken.getExpiresAt());
        entity.setCreatedAt(refreshToken.getCreatedAt());
        entity.setRevoked(refreshToken.isRevoked());
        return entity;
    }
}