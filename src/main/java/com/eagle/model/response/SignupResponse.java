package com.eagle.model.response;

public class SignupResponse {
    private final boolean success;
    private final String message;
    private final String username;
    private final String email;
    private final String dob;      // <-- String, not LocalDate
    private final String address;
    private final String pin;
    private final String phone;

    public SignupResponse(boolean success, String message, String username,
                          String email, String dob, String address, String pin, String phone) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.email = email;
        this.dob = dob;
        this.address = address;
        this.pin = pin;
        this.phone = phone;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDob() { return dob; }
    public String getAddress() { return address; }
    public String getPin() { return pin; }
    public String getPhone() { return phone; }
}
