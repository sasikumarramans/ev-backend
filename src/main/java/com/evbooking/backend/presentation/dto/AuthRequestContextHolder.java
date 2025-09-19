package com.evbooking.backend.presentation.dto;

import org.springframework.web.context.request.RequestAttributes;

public class AuthRequestContextHolder {

    public static Long getCurrentUserId() {
        RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        Object userId = requestAttributes.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
        return userId != null ? (Long) userId : null;
    }

    public static String getCurrentUserRole() {
        RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
        Object userRole = requestAttributes.getAttribute("userRole", RequestAttributes.SCOPE_REQUEST);
        return userRole != null ? (String) userRole : null;
    }
}