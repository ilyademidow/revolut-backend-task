package ru.idemidov.banking.services.moneytransfer.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Account {
    private String accountNumber;
    private Integer currencyCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isBlocked;
    private Double amount;
}
