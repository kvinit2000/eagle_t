package com.eagle.model.request;

import com.eagle.model.response.AccountResponse;
import com.eagle.util.Jsons;

public class CreateAccountRequest {
    private String accountNumber; // optional

    public String getAccountNumber() { return accountNumber; }
    public String toJson() { return Jsons.toJson(this); }
    public static CreateAccountRequest fromJson(String json) { return Jsons.fromJson(json, CreateAccountRequest.class); }
}
