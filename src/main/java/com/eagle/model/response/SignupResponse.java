package com.eagle.model.response;

public class SignupResponse {
    private final boolean success;
    private final String message;
    private final String username;

    public SignupResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
}
