package com.minimarket.security.model;

public class LoginResponse {
    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresInMs;

    public LoginResponse(String token, long expiresInMs) {
        this.token = token;
        this.expiresInMs = expiresInMs;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }
}
