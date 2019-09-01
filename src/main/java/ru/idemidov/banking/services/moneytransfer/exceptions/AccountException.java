package ru.idemidov.banking.services.moneytransfer.exceptions;

public class AccountException extends RuntimeException {
    public AccountException() {
        super();
    }

    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
