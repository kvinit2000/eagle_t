package com.eagle.model.request;

import com.eagle.util.Jsons;
import com.google.gson.Gson;

public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String dob;
    private String address;
    private String pin;
    private String phone;

    private static final Gson GSON = new Gson();

    public SignupRequest() {
        // No-arg constructor required for Gson
    }

    public SignupRequest(String username, String password, String email,
                         String dob, String address, String pin, String phone) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.dob = dob;
        this.address = address;
        this.pin = pin;
        this.phone = phone;
    }

    // ---- Getters & setters ----
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String toJson() { return Jsons.toJson(this); }
    public static SignupRequest fromJson(String json) { return Jsons.fromJson(json, SignupRequest.class); }
}
