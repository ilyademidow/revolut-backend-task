package ru.idemidov.banking.services.moneytransfer.dbmanagment;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import ru.idemidov.banking.services.moneytransfer.models.Account;

import java.sql.*;
import java.util.UUID;

@Log4j2
public class AccountDBManager {
    private String url = "jdbc:h2:mem:default";
    private Connection dbConnection;
    private Statement statement;

    public AccountDBManager() {
        try {
            dbConnection = DriverManager.getConnection(url);
            statement = dbConnection.createStatement();
            createDBStructure(statement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Statement getStatement() {
        return statement;
    }

    public Account getAccountInfo(String accountNumber) throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT account_number, currency_code, start_date, end_date, is_blocked, amount FROM accounts WHERE account_number='" + accountNumber + "'");
        Account result = new Account();
        if (rs.next()) {
            result.setAccountNumber(rs.getString("account_number"));
            result.setCurrencyCode(rs.getInt("currency_code"));
            result.setStartDate(rs.getDate("start_date").toLocalDate());
            result.setEndDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null);
            result.setIsBlocked(rs.getInt("is_blocked"));
            result.setAmount(rs.getDouble("amount"));
        }

        return result;
    }

    /**
     * Debit the sender's account
     *
     * @param accountNumber
     * @param amount        Amount of money in debit account currency
     * @return Unique Transaction Code
     * @throws SQLException
     */
    public String debitAccount(String accountNumber, Double amount) throws SQLException {
        String transCode = UUID.randomUUID().toString();
        statement.execute("INSERT INTO money_transfer_buffer (trans_code, account_number, amount) VALUES ('" + transCode + "', '" + accountNumber + "'," + amount + ")");
        if (Level.DEBUG.equals(log.getLevel())) {
            checkInsertingToBuffer(transCode);
        }
        statement.execute("UPDATE accounts SET amount = amount - " + amount + " WHERE account_number = '" + accountNumber + "'");
        return transCode;
    }

    /**
     * Put transaction to transaction log table
     *
     * @param transCode   Unique Transaction Code
     * @param accountFrom Debit account
     * @param accountTo   Credit account
     * @param amount      amount of money in debit account currency
     * @throws SQLException
     */
    public void logTransfer(String transCode, String accountFrom, String accountTo, Double amount) throws SQLException {
        statement.execute("INSERT INTO money_transfer (trans_code, from_account_number, to_account_number, amount) VALUES ('" + transCode + "', '" + accountFrom + "', '" + accountTo + "', " + amount + ")");
        if (Level.DEBUG.equals(log.getLevel())) {
            checkInsertingToLog(transCode);
        }
    }

    /**
     * Commit the transaction if receiver accepts the money
     *
     * @param transCode Unique Transaction Code
     * @throws SQLException DB errors reason
     */
    public void commitMoneySendTransaction(String transCode) throws SQLException {
        statement.execute("UPDATE money_transfer SET status = 1 WHERE trans_code = '" + transCode + "'");
        if (Level.DEBUG.equals(log.getLevel())) {
            checkUpdatingTransactionStatus(transCode, 1);
        }
        statement.execute("DELETE FROM money_transfer_buffer WHERE trans_code = '" + transCode + "'");
        if (Level.DEBUG.equals(log.getLevel())) {
            checkDeletionFromBuffer(transCode);
        }
    }

    /**
     * Rollback the transaction if something goes wrong
     *
     * @param transCode Unique Transaction Code
     * @throws SQLException DB errors reason
     */
    public void rollBackMoneySendTransaction(String transCode) throws SQLException {
        ResultSet bufferedAmount = statement.executeQuery("SELECT account_number, amount FROM money_transfer_buffer WHERE trans_code = '" + transCode + "'");
        if (bufferedAmount.next()) {
            String accountNumber = bufferedAmount.getString("account_number");
            Double amount = bufferedAmount.getDouble("amount");
            statement.execute("UPDATE money_transfer SET status = 2 WHERE trans_code = '" + transCode + "'");
            if (Level.DEBUG.equals(log.getLevel())) {
                checkUpdatingTransactionStatus(transCode, 2);
            }
            statement.execute("UPDATE accounts SET amount = amount + " + amount + " WHERE account_number = '" + accountNumber + "'");
            statement.execute("DELETE FROM money_transfer_buffer WHERE trans_code = '" + transCode + "'");
            if (Level.DEBUG.equals(log.getLevel())) {
                checkDeletionFromBuffer(transCode);
            }
        } else {
            throw new RuntimeException("Epic fail");
        }
    }

    public void close() {
        try {
            statement.close();
            dbConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkInsertingToBuffer(String transCode) throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT trans_code, account_number, amount FROM money_transfer_buffer WHERE trans_code='" + transCode + "'");
        if (rs.next()) {
            log.debug(String.format("Transaction (trans_code=%s, account_number=%s, amount=%s) has been added to the buffer", rs.getString("trans_code"), rs.getString("account_number"), rs.getDouble("amount")));
        } else {
            log.error("Transaction " + transCode + "has not been added to the buffer");
        }
    }

    private void checkInsertingToLog(String transCode) throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT trans_code FROM money_transfer WHERE trans_code='" + transCode + "'");
        if (rs.next()) {
            log.debug(String.format("Transaction %s has been added to the transaction log", rs.getString("trans_code")));
        } else {
            log.error("Transaction " + transCode + "has not been added to the transaction log");
        }
    }

    private void checkUpdatingTransactionStatus(String transCode, Integer expectedCode) throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT trans_code, status FROM money_transfer WHERE trans_code='" + transCode + "'");
        if (rs.next() && rs.getInt("status") == expectedCode) {
            log.debug(String.format("Transaction %s status has been updated to %d", rs.getString("trans_code"), rs.getInt("status")));
        } else {
            log.error("Transaction " + transCode + " status has not been updated");
        }
    }

    private void checkDeletionFromBuffer(String transCode) throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT 1 FROM money_transfer_buffer WHERE trans_code='" + transCode + "'");
        if (!rs.next()) {
            log.debug("Transaction " + transCode + " has been deleted from the buffer");
        } else {
            log.error("Transaction " + transCode + "has not been deleted from the buffer");
        }
    }

    private final void createDBStructure(Statement statement) {
        try {
            statement.execute("CREATE TABLE accounts(account_number VARCHAR(20) PRIMARY KEY, currency_code INT, start_date DATE, end_date DATE, is_blocked INT, amount DOUBLE );\n" +
                    "CREATE TABLE money_transfer_buffer(id INT PRIMARY KEY AUTO_INCREMENT, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, trans_code VARCHAR(128), account_number VARCHAR(20), amount DOUBLE );\n" +
                    "CREATE TABLE money_transfer(id INT PRIMARY KEY AUTO_INCREMENT, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, trans_code VARCHAR(128), from_account_number VARCHAR(20), to_account_number VARCHAR(20), status INT, amount DOUBLE);");
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public void fillDB() {
        try {
            statement.execute(
                    "INSERT INTO accounts(account_number, currency_code, start_date, amount) VALUES('42368108430020000000', 810, CURRENT_DATE, 5000.0);\n" +
                            "INSERT INTO accounts(account_number, currency_code, start_date, is_blocked, amount) VALUES('42368108430020000001', 810, CURRENT_DATE, 1, 20000.55);\n" +
                            "INSERT INTO accounts(account_number, currency_code, start_date, end_date, amount) VALUES('42368108430020000002', 810, CURRENT_DATE - 5, CURRENT_DATE - 3, 20000.55);\n" +
                            "INSERT INTO accounts(account_number, currency_code, start_date, amount) VALUES('42368108430020000003', 810, CURRENT_DATE, 20000.55);\n" +
                            "INSERT INTO accounts(account_number, currency_code, start_date, amount) VALUES('42368108430021983466', 810, CURRENT_DATE, 10000.05);\n" +
                            "INSERT INTO accounts(account_number, currency_code, start_date, amount) VALUES('42368108430021983463', 810, CURRENT_DATE, 50000.15);");
        } catch (SQLException e) {
            log.error(e);
        }
    }
}
