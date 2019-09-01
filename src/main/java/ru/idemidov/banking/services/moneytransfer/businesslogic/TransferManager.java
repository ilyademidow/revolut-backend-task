package ru.idemidov.banking.services.moneytransfer.businesslogic;

import lombok.extern.log4j.Log4j2;
import ru.idemidov.banking.services.moneytransfer.businesslogic.checkers.*;
import ru.idemidov.banking.services.moneytransfer.dbmanagment.AccountDBManager;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class TransferManager {
    private final AccountDBManager accountDBManager;

    public TransferManager(AccountDBManager accountDBManager) {
        this.accountDBManager = accountDBManager;
    }

    /**
     * Imagine that we send a money to another organization. So they should to accept or decline the transaction.
     * We debit our account and wait
     *
     * @param accountFrom debit account
     * @param accountTo   credit account
     * @param amount      money amount with debit account currency
     * @return Unique Transaction Code
     * @throws AccountException debit isn't allowed reason
     */
    public String sendMoney(String accountFrom, String accountTo, Double amount) throws AccountException {
        String transCode = "error";
        if (checkAccountIsOk(accountFrom, amount)) {
            try {
                // If you want a money transfers in different currencies
                // put here get account currency, get rate, multiply this rate to amount and accountAmount = rate * amount
                transCode = accountDBManager.debitAccount(accountFrom, amount);
                accountDBManager.logTransfer(transCode, accountFrom, accountTo, amount);
            } catch (SQLException e) {
                log.error(e);
            }
        }
        return transCode;
    }

    public void acceptMoneyTransfer(String transCode) {
        try {
            accountDBManager.commitMoneySendTransaction(transCode);
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public void declineMoneyTransfer(String transCode) {
        try {
            accountDBManager.rollBackMoneySendTransaction(transCode);
        } catch (SQLException e) {
            log.error(e);
        }
    }

    /**
     * Check that debit is possible
     *
     * @param accountFrom debit account
     * @param amount      money transfer amount
     * @return if debit is allowed
     */
    private boolean checkAccountIsOk(String accountFrom, Double amount) throws AccountException {
        List<IAccountChecker> accountCheckers = new ArrayList<>();
        try {
            accountCheckers.add(new AccountExistenceChecker(accountDBManager.getAccountInfo(accountFrom)));
            accountCheckers.add(new AccountBlockChecker(accountDBManager.getAccountInfo(accountFrom)));
            accountCheckers.add(new AccountExpiredChecker(accountDBManager.getAccountInfo(accountFrom)));
            accountCheckers.add(new AccountOpenChecker(accountDBManager.getAccountInfo(accountFrom)));
            accountCheckers.add(new MoneyAvailableChecker(accountDBManager.getAccountInfo(accountFrom), amount));
            // Also we can add a limit checkers: single transaction amount, daily count/amount, monthly amount
        } catch (SQLException e) {
            log.error(e);
        }

        return accountCheckers.stream().allMatch(IAccountChecker::check);
    }
}
