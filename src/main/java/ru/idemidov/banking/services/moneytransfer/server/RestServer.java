package ru.idemidov.banking.services.moneytransfer.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.idemidov.banking.services.moneytransfer.businesslogic.AccountManager;
import ru.idemidov.banking.services.moneytransfer.businesslogic.TransferManager;
import ru.idemidov.banking.services.moneytransfer.exceptions.AccountException;
import ru.idemidov.banking.services.moneytransfer.models.Account;
import ru.idemidov.banking.services.moneytransfer.models.MoneyTransferRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;

@Log4j2
public class RestServer {
    private static final Integer PORT_NUMBER = 8080;

    private final TransferManager transferManager;
    private final AccountManager accountManager;

    private HttpServer server;

    public RestServer(TransferManager transferManager, AccountManager accountManager) throws IOException {
        this.transferManager = transferManager;
        this.accountManager = accountManager;

        server = HttpServer.create(new InetSocketAddress(PORT_NUMBER), 0);
        server.createContext("/", (httpExchange -> {
            Response response = new Response();
            response.setRespCode(405); //Method Not Implemented
            log.info("Got " + httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI().getPath());
            if ("POST".equals(httpExchange.getRequestMethod())) {
                response = acceptPostRequest(httpExchange.getRequestURI().getPath(), httpExchange.getRequestBody());
            } else if ("GET".equals(httpExchange.getRequestMethod())) {
                response = acceptGetRequest(httpExchange.getRequestURI().getPath());
            }
            httpExchange.sendResponseHeaders(response.getRespCode(), response.getRespMessage().getBytes().length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(response.getRespMessage().getBytes());
            out.flush();
            httpExchange.close();
        }));
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private Response acceptGetRequest(String url) throws IOException {
        Response response = new Response();
        String resp = "";
        Integer respCode = 404;
        if (url.contains("/accounts/")) {
            respCode = 200;
            String accountNumber = getAccountPathParam(url);
            if (!"".equals(accountNumber)) {
                Account account = accountManager.getAccountInfo(accountNumber);
                resp = account.toString();
                if (account.getAccountNumber() == null) {
                    resp = "No accounts found";
                    log.warn(resp);
                }
            } else {
                resp = "Illegal number format";
            }
        }
        response.setRespCode(respCode);
        response.setRespMessage(resp);
        return response;
    }

    private Response acceptPostRequest(String url, InputStream body) throws IOException {
        Response response = new Response();
        String respMessage = "";
        Integer respCode = 404;
        if ("/operations/sendmoney".equals(url)) {
            ObjectMapper mapper = new ObjectMapper();
            MoneyTransferRequest transferRequest = mapper.readValue(body, MoneyTransferRequest.class);
            respCode = 200;
            try {
                respMessage = transferManager.sendMoney(transferRequest.getFromAccount(), transferRequest.getToAccount(), transferRequest.getAmount());
                log.info("Money has been sent successfully");
            } catch (AccountException e) {
                respMessage = e.getMessage();
                log.warn(respMessage);
            }
        } else if (url.contains("/operations/accept/")) {
            String transCode = getTransCodePathParam(url);
            log.info("Transaction " + transCode + " has been accepted");
            transferManager.acceptMoneyTransfer(transCode);
            respCode = 200;
        } else if (url.contains("/operations/decline/")) {
            String transCode = getTransCodePathParam(url);
            log.info("Transaction " + transCode + " has been declined");
            transferManager.declineMoneyTransfer(transCode);
            respCode = 200;
        }
        response.setRespCode(respCode);
        response.setRespMessage(respMessage);
        return response;
    }

    private String getAccountPathParam(String url) {
        return Arrays.stream(url.split("/")).filter(pathParam -> pathParam.matches("\\d{20}")).findFirst().orElse("");
    }

    private String getTransCodePathParam(String url) {
        return Arrays.stream(url.split("/")).filter(pathParam -> pathParam.matches("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}")).findFirst().orElse("");
    }

    @Getter
    @Setter
    private class Response {
        private Integer respCode;
        private String respMessage;
    }
}
