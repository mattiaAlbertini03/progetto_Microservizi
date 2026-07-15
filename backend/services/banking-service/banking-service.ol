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
    login(loginRequest)(loginResponse) {
        username = loginRequest.username;
        accountId = loginRequest.accountId;

        accountBalance = 100.0;
        rechargeAmount = 50.0;
        heldDeposit = 0.0;
        isLoggedIn = true;

        loginResponse.sessionId = csets.sessionId = new;

        println@Console(
            "User " + username
            + " logged in with account " + accountId
            + ". Initial balance: " + accountBalance
        )()
    };

    while (isLoggedIn) {
        [ charge(chargeRequest)(chargeResponse) {
            amount = chargeRequest.amount;

            if (amount > 0.0) {
                ensureSufficientBalance;
                accountBalance -= amount;
                chargeResponse.success = true
            } else {
                chargeResponse.success = false
            }
        }] {
            println@Console(
                "Charge operation completed for account " + accountId
            )()
        }

        [ holdDeposit(holdRequest)(holdResponse) {
            amount = holdRequest.amount;

            if (amount > 0.0) {
                ensureSufficientBalance;

                accountBalance -= amount;
                heldDeposit += amount;

                holdResponse.success = true
            } else {
                holdResponse.success = false
            }
        }] {
            if (holdResponse.success) {
                println@Console(
                    "Held deposit of " + holdRequest.amount
                    + " for account " + accountId
                    + ". Available balance: " + accountBalance
                )();

                depositIsActive = true;

                while (depositIsActive) {
                    [ charge(activeChargeRequest)(activeChargeResponse) {
                        amount = activeChargeRequest.amount;

                        if (amount > 0.0) {
                            ensureSufficientBalance;
                            accountBalance -= amount;
                            activeChargeResponse.success = true
                        } else {
                            activeChargeResponse.success = false
                        }
                    }]

                    [ releaseDeposit(releaseRequest)(releaseResponse) {
                        accountBalance += heldDeposit;
                        heldDeposit = 0.0;
                        depositIsActive = false;

                        releaseResponse.success = true
                    }]
                }
            }
        }

        [ releaseDeposit(releaseRequest)(releaseResponse) {
            releaseResponse.success = false
        }] {
            println@Console(
                "Deposit release rejected: no active deposit."
            )()
        }

        [ logout(logoutRequest)(logoutResponse) {
            logoutResponse.username = username;
            logoutResponse.accountId = accountId;
            logoutResponse.balance = accountBalance;

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