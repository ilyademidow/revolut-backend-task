package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;

import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;

public class MoneyAvailableChecker implements IAccountChecker {
    private Account account;
    private Double amount;

    public MoneyAvailableChecker(Account account, Double amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public boolean check() {
        if (amount != null && amount > 0 && amount <= account.getAmount()) {
            return true;
        }
        throw new AccountException("Incorrect value (<=0.00) or Transfer amount exceeds the account balance");
    }
}
