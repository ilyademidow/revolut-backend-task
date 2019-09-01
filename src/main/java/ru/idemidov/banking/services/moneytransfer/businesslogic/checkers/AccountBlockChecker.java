package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;


import lombok.AllArgsConstructor;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;

@AllArgsConstructor
public class AccountBlockChecker implements IAccountChecker {
    private Account account;

    @Override
    public boolean check() throws AccountException {
        if (account.getIsBlocked() == null || account.getIsBlocked() == 0) {
            return true;
        }
        throw new AccountException("Account is blocked");
    }
}
