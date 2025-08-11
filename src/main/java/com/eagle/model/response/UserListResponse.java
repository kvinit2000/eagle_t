package com.eagle.model.response;

import com.eagle.util.Jsons;

import java.util.List;

public class UserListResponse {
    private final String message;
    private final List<String> users;

    public UserListResponse(String message, List<String> users) {
        this.message = message;
        this.users = users;
    }

    public String getMessage() { return message; }
    public List<String> getUsers() { return users; }
    public String toJson() { return Jsons.toJson(this); }
    public static UserListResponse fromJson(String json) { return Jsons.fromJson(json, UserListResponse.class); }
}
