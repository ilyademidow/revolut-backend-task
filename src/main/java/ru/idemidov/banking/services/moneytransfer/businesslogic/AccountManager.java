package ru.idemidov.banking.services.moneytransfer.businesslogic;

import lombok.extern.log4j.Log4j2;
import ru.idemidov.banking.services.moneytransfer.dbmanagment.AccountDBManager;
import ru.idemidov.banking.services.moneytransfer.models.Account;

import java.sql.SQLException;

@Log4j2
public class AccountManager {
    private final AccountDBManager accountDBManager;

    public AccountManager(AccountDBManager accountDBManager) {
        this.accountDBManager = accountDBManager;
    }

    public Account getAccountInfo(String accountNumber) {
        try {
            return accountDBManager.getAccountInfo(accountNumber);
        } catch (SQLException e) {
            log.warn(e);
            return null;
        }
    }
}
