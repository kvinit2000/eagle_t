package com.eagle.model.response;

import com.eagle.util.Jsons;

public class AccountResponse {
    private final int id;
    private final String accountNumber;
    private final String balance; // string to keep JSON simple (e.g., "0.00")

    public AccountResponse(int id, String accountNumber, String balance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    public int getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getBalance() { return balance; }
    public String toJson() { return Jsons.toJson(this); }
    public static AccountResponse fromJson(String json) { return Jsons.fromJson(json, AccountResponse.class); }
}
