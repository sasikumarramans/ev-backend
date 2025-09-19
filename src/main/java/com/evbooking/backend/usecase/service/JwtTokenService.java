package com.evbooking.backend.usecase.service;

import com.evbooking.backend.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${spring.security.jwt.secret-key}")
    private String secretKey;

    @Value("${spring.security.jwt.expiration:3600000}") // 1 hour default
    private long accessTokenExpiration;

    private static final long REFRESH_TOKEN_EXPIRATION = 30L * 24 * 60 * 60 * 1000; // 30 days

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("phoneNumber", user.getPhoneNumber());
        claims.put("role", user.getRole().name());
        claims.put("status", user.getStatus().name());

        if (user.getEmail() != null) {
            claims.put("email", user.getEmail());
        }

        if (user.getFirstName() != null) {
            claims.put("firstName", user.getFirstName());
        }

        if (user.getLastName() != null) {
            claims.put("lastName", user.getLastName());
        }

        return createToken(claims, user.getPhoneNumber(), accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("tokenType", "REFRESH");
        claims.put("tokenId", UUID.randomUUID().toString());

        return createToken(claims, user.getPhoneNumber(), REFRESH_TOKEN_EXPIRATION);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("ev-booking-backend")
                .signWith(getSigningKey())
                .compact();
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    public String extractPhoneNumber(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("phoneNumber", String.class);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
            throw new RuntimeException("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            logger.debug("JWT token is unsupported: {}", e.getMessage());
            throw new RuntimeException("JWT token is unsupported");
        } catch (MalformedJwtException e) {
            logger.debug("JWT token is malformed: {}", e.getMessage());
            throw new RuntimeException("JWT token is malformed");
        } catch (SecurityException e) {
            logger.debug("JWT signature validation failed: {}", e.getMessage());
            throw new RuntimeException("JWT signature validation failed");
        } catch (IllegalArgumentException e) {
            logger.debug("JWT token is invalid: {}", e.getMessage());
            throw new RuntimeException("JWT token is invalid");
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token, String subject) {
        try {
            final String extractedSubject = extractSubject(token);
            return (extractedSubject.equals(subject) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public LocalDateTime getExpirationAsLocalDateTime(String token) {
        Date expiration = extractExpiration(token);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public String generateClientApiToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "CLIENT_API");
        claims.put("clientId", "ev-booking-mobile");
        claims.put("tokenId", UUID.randomUUID().toString());

        // Long expiration for client API token (1 year)
        long clientTokenExpiration = 365L * 24 * 60 * 60 * 1000;

        return createToken(claims, "ev-booking-client", clientTokenExpiration);
    }

    public boolean isClientApiToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get("tokenType", String.class);
            return "CLIENT_API".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }
}