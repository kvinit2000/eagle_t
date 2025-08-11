package com.eagle.model.request;

import com.eagle.util.Jsons;

public class PatchUserRequest {
    private String email;
    private String dob;      // yyyy-MM-dd
    private String address;
    private String pin;
    private String phone;

    public String getEmail() { return email; }
    public String getDob() { return dob; }
    public String getAddress() { return address; }
    public String getPin() { return pin; }
    public String getPhone() { return phone; }
    public String toJson() { return Jsons.toJson(this); }
    public static PatchUserRequest fromJson(String json) { return Jsons.fromJson(json, PatchUserRequest.class); }
}
