package com.eagle.model.request;

public class TransactionRequest {

    private String amount;  // Keep as String so we can trim and validate later
    private String type;    // "DEPOSIT" or "WITHDRAW"
    private String description;

    public TransactionRequest() {
    }

    public TransactionRequest(String amount, String type, String description) {
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
