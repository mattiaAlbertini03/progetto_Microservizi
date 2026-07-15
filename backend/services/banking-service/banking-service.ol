include "console.iol"
include "banking-interface.iol"

inputPort MyInput {
    Location: "socket://0.0.0.0:8080/"
    Protocol: soap {
        .wsdl = "./BankingService.wsdl";
        .wsdl.port = "BankingServicePort"
    }
    Interfaces: BankService
}

/*
 * Simulates an automatic recharge until the account has enough funds
 * to complete the requested operation.
 */
define ensureSufficientBalance {
    while (amount > accountBalance) {
        accountBalance += rechargeAmount
    }
}

/*
 * Applies a standard charge to the account.
 */
define applyCharge {
    amount = request.amount;

    if (amount > 0.0) {
        ensureSufficientBalance;
        accountBalance -= amount;
        response.success = true
    } else {
        response.success = false
    }
}

execution {
    concurrent
}

cset {
    sessionId:
        SessionRequest.sessionId
        MonetaryOperationRequest.sessionId
}

main {
    login(request)(response) {
        username = request.username;
        accountId = request.accountId;

        /*
         * Temporary simulated account data.
         * Persistence will later load these values using accountId.
         */
        accountBalance = 100.0;
        rechargeAmount = 50.0;
        heldDeposit = 0.0;

        isLoggedIn = true;

        response.sessionId = csets.sessionId = new;

        println@Console(
            "User " + username
            + " logged in with account " + accountId
            + ". Initial balance: " + accountBalance
        )()
    };

    while (isLoggedIn) {
        [ charge(request)(response) {
            applyCharge
        }] {
            if (response.success) {
                println@Console(
                    "Charged " + request.amount
                    + " from account " + accountId
                    + ". New balance: " + accountBalance
                )()
            } else {
                println@Console(
                    "Charge rejected: amount must be greater than zero."
                )()
            }
        }

        [ holdDeposit(request)(response) {
            amount = request.amount;

            if (amount > 0.0) {
                ensureSufficientBalance;

                accountBalance -= amount;
                heldDeposit += amount;

                response.success = true
            } else {
                response.success = false
            }
        }] {
            if (response.success) {
                println@Console(
                    "Held deposit of " + request.amount
                    + " for account " + accountId
                    + ". Available balance: " + accountBalance
                )();

                depositIsActive = true;

                while (depositIsActive) {
                    [ charge(request)(response) {
                        applyCharge
                    }] {
                        if (response.success) {
                            println@Console(
                                "Charged " + request.amount
                                + " while deposit is active."
                                + " New balance: " + accountBalance
                            )()
                        } else {
                            println@Console(
                                "Charge rejected: amount must be greater than zero."
                            )()
                        }
                    }

                    [ releaseDeposit(request)(response) {
                        accountBalance += heldDeposit;
                        heldDeposit = 0.0;
                        depositIsActive = false;

                        response.success = true
                    }] {
                        println@Console(
                            "Deposit released for account " + accountId
                            + ". New balance: " + accountBalance
                        )()
                    }
                }
            } else {
                println@Console(
                    "Deposit hold rejected: amount must be greater than zero."
                )()
            }
        }

        [ releaseDeposit(request)(response) {
            /*
             * No deposit is currently active.
             */
            response.success = false
        }] {
            println@Console(
                "Deposit release rejected: no active deposit."
            )()
        }

        [ logout(request)(response) {
            response.username = username;
            response.accountId = accountId;
            response.balance = accountBalance;

            isLoggedIn = false
        }] {
            println@Console(
                "User " + username
                + " logged out from account " + accountId
                + ". Final balance: " + accountBalance
            )()
        }
    }
}