package com.evbooking.backend.domain.repository;

import com.evbooking.backend.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    User save(User user);
    void deleteById(Long id);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}