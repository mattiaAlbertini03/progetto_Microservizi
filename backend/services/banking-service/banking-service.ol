include "console.iol"
include "database.iol"
include "runtime.iol"
include "banking-interface.iol"

inputPort MyInput {
    Location: "socket://0.0.0.0:8080/"
    Protocol: soap {
        .wsdl = "./BankingService.wsdl";
        .wsdl.port = "MyInputServicePort"
    }
    Interfaces: BankService
}

inputPort WebInput {
    Location: "socket://0.0.0.0:8000/"
    Protocol: http {
        .format = "json"
        .osc << {
            getAccount << { .alias = "bank/accounts/%!{userId}" .method = "get" }
            registerAccount << { .alias = "bank/accounts/%!{userId}" .method = "post" }
            rechargeAccount << { .alias = "bank/accounts/%!{userId}/recharge" .method = "post" }
            openBankSession << { .alias = "bank/accounts/%!{userId}/session" .method = "post" }
        }
        .default = "default"
    }
    Interfaces: BankRestService
}

execution { concurrent }

define loadAccountByUserId {
    query@Database(
        "SELECT user_id, username, account_id, balance FROM accounts WHERE user_id=:user_id" {
            .user_id = request.userId
        }
    )(accountRows)
}

define loadAccountByAccountId {
    query@Database(
        "SELECT user_id, username, account_id, balance FROM accounts WHERE account_id=:account_id" {
            .account_id = request.accountId
        }
    )(accountRows)
}

define mapAccountResponse {
    println@Console("banking-service: account rows=" + #accountRows.row)();
    if (#accountRows.row == 1) {
        response.connected = true;
        response.userId = accountRows.row[0].user_id;
        response.username = accountRows.row[0].username;
        response.accountId = accountRows.row[0].account_id;
        response.balance = accountRows.row[0].balance
    } else {
        response.connected = false
    }
}

define createSession {
    sessionId = new;
    update@Database(
        "INSERT INTO bank_sessions(session_id, user_id, held_deposit) VALUES (:session_id, :user_id, 0)" {
            .session_id = sessionId,
            .user_id = accountRows.row[0].user_id
        }
    )(updatedRows);
    response.sessionId = sessionId
}

init {
    getenv@Runtime("BANK_DB_HOST")(dbHost);
    getenv@Runtime("BANK_DB_PORT")(dbPort);
    getenv@Runtime("BANK_DB_NAME")(dbName);
    getenv@Runtime("BANK_DB_USER")(dbUser);
    getenv@Runtime("BANK_DB_PASSWORD")(dbPassword);

    with (connectionInfo) {
        .username = dbUser;
        .password = dbPassword;
        .host = dbHost;
        .port = int(dbPort);
        .database = dbName;
        .driver = "postgresql";
        .toLowerCase = true
    };
    connect@Database(connectionInfo)();
    println@Console("banking-service: database connected")();

    update@Database(
        "CREATE TABLE IF NOT EXISTS accounts (" +
        "user_id VARCHAR(255) PRIMARY KEY, username VARCHAR(255) NOT NULL, " +
        "account_id VARCHAR(255) NOT NULL UNIQUE, balance DOUBLE PRECISION NOT NULL DEFAULT 0)"
    )(schemaResult);
    update@Database(
        "CREATE TABLE IF NOT EXISTS bank_sessions (" +
        "session_id VARCHAR(255) PRIMARY KEY, user_id VARCHAR(255) NOT NULL REFERENCES accounts(user_id), " +
        "held_deposit DOUBLE PRECISION NOT NULL DEFAULT 0)"
    )(schemaResult)
}

main {
    [ getAccount(request)(response) {
        println@Console("banking-service: getAccount userId=" + request.userId)();
        loadAccountByUserId;
        mapAccountResponse
    }]

    [ registerAccount(request)(response) {
        println@Console(
            "banking-service: registerAccount userId=" + request.userId +
            " fields-present=" + (request.cardNumber != "") + "," +
            (request.cardHolder != "") + "," + (request.expiry != "") + "," +
            (request.cvv != "")
        )();
        update@Database(
            "INSERT INTO accounts(user_id, username, account_id, balance) VALUES (:user_id, :username, :account_id, 0) ON CONFLICT (user_id) DO UPDATE SET username=EXCLUDED.username, account_id=EXCLUDED.account_id" {
                .user_id = request.userId,
                .username = request.username,
                .account_id = request.cardNumber
            }
        )(updatedRows);
        println@Console("banking-service: registerAccount updatedRows=" + updatedRows)();
        loadAccountByUserId;
        mapAccountResponse
    }]

    [ rechargeAccount(request)(response) {
        println@Console("banking-service: rechargeAccount userId=" + request.userId)();
        if (request.amount > 0.0) {
            update@Database(
                "UPDATE accounts SET balance=balance+:amount WHERE user_id=:user_id" {
                    .amount = request.amount,
                    .user_id = request.userId
                }
            )(updatedRows)
        };
        loadAccountByUserId;
        mapAccountResponse
    }]

    [ openBankSession(request)(response) {
        println@Console("banking-service: openBankSession accountId-present=" + (request.accountId != ""))();
        loadAccountByAccountId;
        if (#accountRows.row == 1) { createSession }
    }]

    [ login(request)(response) {
        loadAccountByAccountId;
        if (#accountRows.row == 1) { createSession }
    }]

    [ checkFunds(request)(response) {
        query@Database(
            "SELECT EXISTS(SELECT 1 FROM accounts a JOIN bank_sessions s ON s.user_id=a.user_id WHERE s.session_id=:session_id AND a.balance>=:amount) AS enough" {
                .session_id = request.sessionId,
                .amount = request.amount
            }
        )(fundRows);
        response.success = #fundRows.row == 1 && fundRows.row[0].enough
    }]

    [ charge(request)(response) {
        if (request.amount > 0.0) {
            update@Database(
                "UPDATE accounts a SET balance=a.balance-:amount FROM bank_sessions s WHERE s.session_id=:session_id AND a.user_id=s.user_id AND a.balance>=:amount" {
                    .amount = request.amount,
                    .session_id = request.sessionId
                }
            )(updatedRows);
            response.success = updatedRows == 1
        } else { response.success = false }
    }]

    [ holdDeposit(request)(response) {
        if (request.amount > 0.0) {
            update@Database(
                "WITH debited AS (UPDATE accounts a SET balance=a.balance-:amount FROM bank_sessions s WHERE s.session_id=:session_id AND a.user_id=s.user_id AND a.balance>=:amount RETURNING a.user_id) UPDATE bank_sessions s SET held_deposit=s.held_deposit+:amount FROM debited d WHERE s.session_id=:session_id AND s.user_id=d.user_id" {
                    .amount = request.amount,
                    .session_id = request.sessionId
                }
            )(updatedRows);
            response.success = updatedRows == 1
        } else { response.success = false }
    }]

    [ releaseDeposit(request)(response) {
        update@Database(
            "WITH released AS (UPDATE bank_sessions SET held_deposit=0 WHERE session_id=:session_id AND held_deposit>0 RETURNING user_id, held_deposit) UPDATE accounts a SET balance=a.balance+r.held_deposit FROM released r WHERE a.user_id=r.user_id" {
                .session_id = request.sessionId
            }
        )(updatedRows);
        response.success = updatedRows == 1
    }]

    [ logout(request)(response) {
        query@Database(
            "SELECT a.username, a.account_id, a.balance FROM accounts a JOIN bank_sessions s ON s.user_id=a.user_id WHERE s.session_id=:session_id" {
                .session_id = request.sessionId
            }
        )(accountRows);
        if (#accountRows.row == 1) {
            response.username = accountRows.row[0].username;
            response.accountId = accountRows.row[0].account_id;
            response.balance = accountRows.row[0].balance;
            update@Database("DELETE FROM bank_sessions WHERE session_id=:session_id" {
                .session_id = request.sessionId
            })(updatedRows)
        }
    }]
}
