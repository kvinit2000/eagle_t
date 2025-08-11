package com.eagle.model.response;

import com.eagle.util.Jsons;

public class ErrorResponse {
    private String error;
    private String details;

    public ErrorResponse() {}
    public ErrorResponse(String error, String details) {
        this.error = error; this.details = details;
    }

    public String toJson() { return Jsons.toJson(this); }
    public static ErrorResponse fromJson(String json) { return Jsons.fromJson(json, ErrorResponse.class); }

    public String getError() { return error; }
    public String getDetails() { return details; }
}
