# Home Back-End task for a Revolut interview

## Task: 
Implement RESTful API for money send

## Overview:
I've kept into account that it should be simple so I understand but haven't implement another bank attributes, a transaction currency (exchange currency rates), a limits for a transaction count, amount, daily, monthly. Also I don't use an embedded web server on purpose.

##### Main idea
I imagine that we have an account and we need to send a money to another company account. So we know about credit account nothing. I implement a debit transaction and it can be commit or rolled  back

## Solution:
Front-End or something invokes a REST method `POST /operations/sendmoney`. Then we pretend send transaction (with unique transaction code) to another bank. And another bank can accept our transaction by invocation a REST method `POST /operations/accept/{transaction_code}` or it can decline our transaction by `POST /operations/decline/{transaction_code}`. Also Front-End or something can get all info about an account `GET /accounts/{account_number}`

**I've put an additional checks of the DB tables in debug logs to be sure that all background steps are passed**

To run this API server run<br>
```java -jar money-transfer-api-server.jar```

To check it with prepared data you can download the project and run the tests. Or you can run<br>
```java -jar money-transfer-api-server.jar fillDB```<br>
and then run bellowed comands step by step:
* Check the account balance `curl -X GET http://localhost:8080/accounts/42368108430021983466`
* Send money `curl -d '{"fromAccount": "42368108430021983466", "toAccount":"42368108430021983405", "amount" : 2000.0}' -X POST http://localhost:8080/operations/sendmoney` you should see a transaction code
* Accept the money transfer `curl -X POST http://localhost:8080/operations/accept/{trans_code}` (replace `{trans_code}` to got transaction code)
* Check the account balance `curl -X GET http://localhost:8080/accounts/42368108430021983466`