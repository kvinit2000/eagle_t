package com.eagle.model.response;

import com.eagle.util.Jsons;

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
    public String toJson() { return Jsons.toJson(this); }
    public static PingResponse fromJson(String json) { return Jsons.fromJson(json, PingResponse.class); }
}
