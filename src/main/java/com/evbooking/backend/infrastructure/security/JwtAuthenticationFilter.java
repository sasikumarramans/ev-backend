package com.evbooking.backend.infrastructure.security;

import com.evbooking.backend.usecase.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;

    @Value("${app.security.client-token:Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9}")
    private String clientToken;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Check if this is a public endpoint that doesn't require authentication
            String requestPath = request.getRequestURI();
            if (isPublicEndpoint(requestPath)) {
                // Still check for client token on auth endpoints
                if (requestPath.startsWith("/api/auth/")) {
                    if (!validateClientToken(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Invalid or missing client token\"}");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
                return;
            }

            // Get JWT token from request
            String jwt = getJwtFromRequest(request);

            if (jwt != null && jwtTokenService.validateToken(jwt)) {
                // Extract user information from JWT
                String phoneNumber = jwtTokenService.extractPhoneNumber(jwt);
                Long userId = jwtTokenService.extractUserId(jwt);
                String role = jwtTokenService.extractRole(jwt);

                if (phoneNumber != null && userId != null) {
                    // Create authentication object
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(phoneNumber, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set user ID in request attribute for easy access in controllers
                    request.setAttribute("userId", userId);
                    request.setAttribute("userRole", role);

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                logger.debug("JWT token is null, empty, or invalid for request: {}", requestPath);
            }

        } catch (Exception ex) {
            logger.error("Cannot set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateClientToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        // Check against configured client token from environment
        if (clientToken.equals(authHeader)) {
            return true;
        }

        // Also check if it's a valid JWT client token with Bearer prefix
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtTokenService.validateToken(token) &&
                       jwtTokenService.isClientApiToken(token);
            } catch (Exception e) {
                logger.debug("Client token validation failed: {}", e.getMessage());
            }
        }

        return false;
    }

    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.startsWith("/api/auth/") ||
               requestPath.startsWith("/api/public/") ||
               requestPath.equals("/api/actuator/health") ||
               requestPath.equals("/api/actuator/info") ||
               requestPath.startsWith("/api/swagger-ui/") ||
               requestPath.equals("/api/swagger-ui.html") ||
               requestPath.startsWith("/api/v3/api-docs/") ||
               requestPath.equals("/error");
    }
}