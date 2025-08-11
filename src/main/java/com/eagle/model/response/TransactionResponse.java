package com.eagle.model.response;

import com.eagle.util.Jsons;

public class TransactionResponse {
    private final int id;
    private final String type;
    private final String amount;
    private final String balanceAfter;
    private final String createdAt;

    public TransactionResponse(int id, String type, String amount, String balanceAfter, String createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getAmount() { return amount; }
    public String getBalanceAfter() { return balanceAfter; }
    public String getCreatedAt() { return createdAt; }
    public String toJson() { return Jsons.toJson(this); }
    public static TransactionResponse fromJson(String json) { return Jsons.fromJson(json, TransactionResponse.class); }
}
