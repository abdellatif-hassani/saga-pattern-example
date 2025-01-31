package com.hassani.transactionservice.model;

import lombok.Data;

@Data
public class InitiateTransferRequest {
    private String fromAccount;
    private String toAccount;
    private Double amount;
}
