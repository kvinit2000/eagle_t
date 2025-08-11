package com.eagle.model.request;

import com.eagle.util.Jsons;

public class LoginRequest {
    private String username;
    private String password;

    public String getUsername(){ return username; }
    public void setUsername(String username){ this.username = username; }
    public String getPassword(){ return password; }
    public void setPassword(String password){ this.password = password; }
    public String toJson() { return Jsons.toJson(this); }
    public static LoginRequest fromJson(String json) { return Jsons.fromJson(json, LoginRequest.class); }
}
