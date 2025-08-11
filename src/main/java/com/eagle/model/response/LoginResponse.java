package com.eagle.model.response;

import com.eagle.util.Jsons;

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
    public String toJson() { return Jsons.toJson(this); }
    public static LoginResponse fromJson(String json) { return Jsons.fromJson(json, LoginResponse.class); }
}
