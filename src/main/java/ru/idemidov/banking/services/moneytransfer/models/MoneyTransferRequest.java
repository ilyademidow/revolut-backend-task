package ru.idemidov.banking.services.moneytransfer.models;

import lombok.Data;

@Data
public class MoneyTransferRequest {
    private String fromAccount;
    private String toAccount;
    private Double amount;
}
