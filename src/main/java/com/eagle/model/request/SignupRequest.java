package com.eagle.model.request;

public class SignupRequest {
    private String username;  // required
    private String password;  // required

    // Optional extended fields
    private String email;     // optional
    private String dob;       // optional, ISO-8601 (yyyy-MM-dd)
    private String address;   // optional
    private String pin;       // optional
    private String phone;     // optional

    public SignupRequest() {}

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
}
