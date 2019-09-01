package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;

import lombok.AllArgsConstructor;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;

import java.time.LocalDate;

@AllArgsConstructor
public class AccountExpiredChecker implements IAccountChecker {
    private Account account;

    @Override
    public boolean check() throws AccountException {
        if (account.getEndDate() == null || LocalDate.now().isBefore(account.getEndDate())) {
            return true;
        }
        throw new AccountException("Account is expired");
    }
}
