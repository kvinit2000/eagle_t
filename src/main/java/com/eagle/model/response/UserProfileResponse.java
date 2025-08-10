package com.eagle.model.response;

public class UserProfileResponse {
    private final String username;
    private final String email;
    private final String dob;
    private final String address;
    private final String pin;
    private final String phone;

    public UserProfileResponse(String username, String email, String dob, String address, String pin, String phone) {
        this.username = username;
        this.email = email;
        this.dob = dob;
        this.address = address;
        this.pin = pin;
        this.phone = phone;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDob() { return dob; }
    public String getAddress() { return address; }
    public String getPin() { return pin; }
    public String getPhone() { return phone; }
}
