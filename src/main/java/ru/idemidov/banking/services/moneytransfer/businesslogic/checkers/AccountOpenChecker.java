package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;

import lombok.AllArgsConstructor;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;

import java.time.LocalDate;

@AllArgsConstructor
public class AccountOpenChecker implements IAccountChecker {
    private Account account;

    @Override
    public boolean check() throws AccountException {
        if (LocalDate.now().isEqual(account.getStartDate()) || LocalDate.now().isAfter(account.getStartDate())) {
            return true;
        }
        throw new AccountException("Account is not opened");
    }
}
