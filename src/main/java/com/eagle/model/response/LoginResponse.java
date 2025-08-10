package com.eagle.model.response;

public class LoginResponse {
    private final String token;
    private final long expiresInSeconds;
    private final String message;

    public LoginResponse(String token, long expiresInSeconds, String message) {
        this.token = token; this.expiresInSeconds = expiresInSeconds; this.message = message;
    }
    public String getToken(){ return token; }
    public long getExpiresInSeconds(){ return expiresInSeconds; }
    public String getMessage(){ return message; }
}
