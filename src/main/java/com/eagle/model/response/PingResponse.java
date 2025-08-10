package com.eagle.model.response;

public class PingResponse {
    private String message;
    private long timestamp;

    public PingResponse(String message, long timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
