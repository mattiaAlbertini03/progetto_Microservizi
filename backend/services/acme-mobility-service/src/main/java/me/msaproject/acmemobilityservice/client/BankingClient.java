package me.msaproject.acmemobilityservice.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import me.msaproject.acmemobilityservice.dto.bank.BankAccountResponse;
import me.msaproject.acmemobilityservice.dto.bank.CardRegistrationRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@ApplicationScoped
public class BankingClient {

    private final Client client = ClientBuilder.newClient();
    private final String bankingServiceUrl;

    @Inject
    public BankingClient(@ConfigProperty(
            name = "banking-service.url",
            defaultValue = "http://banking-service.acme:8000") String bankingServiceUrl) {
        this.bankingServiceUrl = bankingServiceUrl;
    }

    public BankAccountResponse getAccount(String userId) {
        BankAccountResponse account = client.target(bankingServiceUrl)
                .path("getAccount").request()
                .post(Entity.json(Map.of("userId", userId)), BankAccountResponse.class);
        return withSession(account);
    }

    public BankAccountResponse registerAccount(
            String userId,
            String username,
            CardRegistrationRequest request
    ) {
        BankAccountResponse account = client.target(bankingServiceUrl)
                .path("registerAccount").request()
                .post(Entity.json(Map.of(
                        "userId", userId,
                        "username", username,
                        "cardNumber", request.cardNumber(),
                        "cardHolder", request.cardHolder(),
                        "expiry", request.expiry(),
                        "cvv", request.cvv())), BankAccountResponse.class);
        return withSession(account);
    }

    public BankAccountResponse recharge(String userId, double amount) {
        BankAccountResponse account = client.target(bankingServiceUrl)
                .path("rechargeAccount").request()
                .post(Entity.json(Map.of("userId", userId, "amount", amount)), BankAccountResponse.class);
        return withSession(account);
    }

    private BankAccountResponse withSession(BankAccountResponse account) {
        if (!account.connected()) return account;

        @SuppressWarnings("unchecked")
        Map<String, Object> session = client.target(bankingServiceUrl)
                .path("openBankSession").request()
                .post(Entity.json(Map.of(
                        "username", account.username(),
                        "accountId", account.accountId())), Map.class);
        return new BankAccountResponse(
                true,
                account.userId(),
                account.username(),
                account.accountId(),
                account.balance(),
                (String) session.get("sessionId"));
    }
}
