package ru.idemidov.banking.services.moneytransfer;

import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;

@Log4j2
public class MainApplicationTest {
    private Pattern uuidPattern = Pattern.compile("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}");
    @BeforeClass
    public static void runWebServer() throws IOException {
        MainApplication.main(new String[]{"fillDB"});
    }

    @Test
    public void whenExceedBalance_thenGetError() throws IOException {
        String message = sendMoney("{\"fromAccount\": \"42368108430020000000\", \"toAccount\":\"42368108430021983405\", \"amount\" : 200000.0}");
        Assert.assertEquals("Incorrect value (<=0.00) or Transfer amount exceeds the account balance", message);
    }

    @Test
    public void whenDebitBlockedAccount_thenGetError() throws IOException {
        // Amount exceeds the balance but first reason that account is blocked
        String message = sendMoney("{\"fromAccount\": \"42368108430020000001\", \"toAccount\":\"42368108430021983405\", \"amount\" : 500000.0}");
        Assert.assertEquals("Account is blocked", message);
    }

    @Test
    public void whenDebitExpiredAccount_thenGetError() throws IOException {
        // Amount exceeds the balance but first reason that account is expired
        String message = sendMoney("{\"fromAccount\": \"42368108430020000002\", \"toAccount\":\"42368108430021983405\", \"amount\" : 500000.0}");
        Assert.assertEquals("Account is expired", message);
    }

    @Test
    public void whenDeclineMoneyTransfer_thenBalanceRollsBack() throws IOException {
        String transCode = sendMoney("{\"fromAccount\": \"42368108430021983466\", \"toAccount\":\"42368108430021983405\", \"amount\" : 2000.0}");
        Assert.assertTrue(uuidPattern.matcher(transCode).matches());
        declineSendMoney(transCode);
        Assert.assertTrue(getAccountInfo("42368108430021983466").contains("amount=10000.05"));
    }

    @Test
    public void whenAcceptMoneyTransfer_thenBalanceIsChanged() throws IOException {
        String transCode = sendMoney("{\"fromAccount\": \"42368108430021983463\", \"toAccount\":\"42368108430021983405\", \"amount\" : 20000.15}");
        acceptSendMoney(transCode);
        Assert.assertTrue(getAccountInfo("42368108430021983463").contains("amount=30000.0"));
    }

    @Test
    public void whenRequestWrongAccountNumber_thenSendError() throws IOException {
        URLConnection wrongAccountURL = new URL("http://localhost:8080/accounts/42368108430021981111").openConnection();
        HttpURLConnection httpClient = (HttpURLConnection) wrongAccountURL;
        httpClient.setRequestMethod("GET");
        httpClient.setDoOutput(true);
        httpClient.connect();
        String wrongAccountResult = new Scanner(httpClient.getInputStream()).nextLine();
        log.info(wrongAccountResult);
        Assert.assertTrue(wrongAccountResult.contains("No accounts found"));
        httpClient.disconnect();
    }

    private String sendMoney(String jsonString) throws IOException {
        URLConnection sendMoneyURL = new URL("http://localhost:8080/operations/sendmoney").openConnection();
        HttpURLConnection httpClient = (HttpURLConnection) sendMoneyURL;
        httpClient.setRequestMethod("POST");
        httpClient.setDoOutput(true);
        byte[] json = jsonString.getBytes(StandardCharsets.UTF_8);
        httpClient.setFixedLengthStreamingMode(json.length);
        httpClient.connect();
        try (OutputStream os = httpClient.getOutputStream()) {
            os.write(json);
        }
        String transCode = new Scanner(httpClient.getInputStream()).nextLine();
        log.info(transCode);
        httpClient.disconnect();
        return transCode;
    }

    private void declineSendMoney(String transCode) throws IOException {
        URLConnection declineSendMoneyURL = new URL("http://localhost:8080/operations/decline/" + transCode).openConnection();
        HttpURLConnection httpClient = (HttpURLConnection) declineSendMoneyURL;
        httpClient.setDoOutput(true);
        httpClient.setFixedLengthStreamingMode(1);
        httpClient.connect();
        try (OutputStream os = httpClient.getOutputStream()) {
            os.write(0);
        }
        httpClient.disconnect();
    }

    private void acceptSendMoney(String transCode) throws IOException {
        URLConnection acceptSendMoneyURL = new URL("http://localhost:8080/operations/accept/" + transCode).openConnection();
        HttpURLConnection httpClient = (HttpURLConnection) acceptSendMoneyURL;
        httpClient.setDoOutput(true);
        httpClient.setFixedLengthStreamingMode(1);
        httpClient.connect();
        try (OutputStream os = httpClient.getOutputStream()) {
            os.write(0);
        }
        httpClient.disconnect();
    }

    private String getAccountInfo(String accountNumber) throws IOException {
        URLConnection accountURL = new URL("http://localhost:8080/accounts/" + accountNumber).openConnection();
        HttpURLConnection httpClient = (HttpURLConnection) accountURL;
        httpClient.setRequestMethod("GET");
        httpClient.connect();
        String account = new Scanner(httpClient.getInputStream()).nextLine();
        log.info(account);
        httpClient.disconnect();
        return account;
    }
}
