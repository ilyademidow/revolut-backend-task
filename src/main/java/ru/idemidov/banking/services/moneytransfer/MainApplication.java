package ru.idemidov.banking.services.moneytransfer;

import lombok.extern.log4j.Log4j2;
import ru.idemidov.banking.services.moneytransfer.businesslogic.AccountManager;
import ru.idemidov.banking.services.moneytransfer.businesslogic.TransferManager;
import ru.idemidov.banking.services.moneytransfer.dbmanagment.AccountDBManager;
import ru.idemidov.banking.services.moneytransfer.server.RestServer;

import java.io.IOException;

@Log4j2
public class MainApplication {

    private static final String FILL_DB_COMMAND = "fillDB";

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || !FILL_DB_COMMAND.equals(args[0])) {
            log.info("App is started with empty DB data. If you want to fill DB the default data so run it with argument = FILL_DB_COMMAND\n\n");
        }
        log.info("Connecting DB...");
        final AccountDBManager accountDBManager = new AccountDBManager();
        if (args.length >= 1 && FILL_DB_COMMAND.equals(args[0])) {
            log.info("Filling DB...");
            accountDBManager.fillDB();
            log.info("DB has been filled");
        }
        final AccountManager accountManager = new AccountManager(accountDBManager);
        final TransferManager transferManager = new TransferManager(accountDBManager);
        log.info("DB is connected");
        log.info("Starting server...");
        final RestServer restServer = new RestServer(transferManager, accountManager);
        restServer.start();
        log.info("Server is started");
    }
}
