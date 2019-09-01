package ru.idemidov.banking.services.moneytransfer.businesslogic.checkers;

import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;

public interface IAccountChecker {
    boolean check() throws AccountException;
}
