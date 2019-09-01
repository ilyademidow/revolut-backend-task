package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;

import lombok.AllArgsConstructor;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;

@AllArgsConstructor
public class AccountExistenceChecker implements IAccountChecker {
    private Account account;

    @Override
    public boolean check() throws AccountException {
        if (account != null && account.getAccountNumber() != null && account.getAccountNumber() != "") {
            return true;
        }
        throw new AccountException("Account does not exist");
    }
}