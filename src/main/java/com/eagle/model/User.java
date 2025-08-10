package com.eagle.model;

import java.sql.Date;

public class User {
    private int id;              // DB identity (generated)
    private String username;     // unique
    private String password;     // plain for now (we’ll hash later)
    private String email;        // unique
    private Date dob;            // yyyy-MM-dd
    private String address;      // full address string
    private String pin;          // postal/zip code
    private String phone;        // phone number

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
