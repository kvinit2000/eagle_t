package com.eagle.model.response;

public class SignupResponse {
    private final boolean success;
    private final String message;

    public SignupResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
