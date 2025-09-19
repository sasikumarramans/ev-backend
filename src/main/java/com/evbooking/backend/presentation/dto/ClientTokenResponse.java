package com.evbooking.backend.presentation.dto;

public class ClientTokenResponse {
    private String clientToken;
    private String message;

    public ClientTokenResponse() {}

    public ClientTokenResponse(String clientToken, String message) {
        this.clientToken = clientToken;
        this.message = message;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}