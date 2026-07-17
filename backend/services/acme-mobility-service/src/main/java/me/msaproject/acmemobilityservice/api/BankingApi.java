package me.msaproject.acmemobilityservice.api;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.auth.AuthenticatedUser;
import me.msaproject.acmemobilityservice.auth.JwtService;
import me.msaproject.acmemobilityservice.auth.PendingBankConnectionService;
import me.msaproject.acmemobilityservice.client.BankingClient;
import me.msaproject.acmemobilityservice.dto.bank.CardRegistrationRequest;
import me.msaproject.acmemobilityservice.dto.bank.BankLoginRequest;
import me.msaproject.acmemobilityservice.dto.bank.RechargeRequest;

import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Path("/bank/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BankingApi {

    @Inject BankingClient bankingClient;
    @Inject JwtService jwtService;
    @Inject CamundaClient camundaClient;
    @Inject PendingBankConnectionService pendingBankConnections;

    @GET
    public Response getAccount(@HeaderParam("Authorization") String authorizationHeader) {
        AuthenticatedUser user = jwtService.authenticate(authorizationHeader);
        return Response.ok(bankingClient.getAccount(user.userId())).build();
    }

    @POST
    @Path("/connect")
    public Response connect(
            CardRegistrationRequest request,
            @HeaderParam("Authorization") String authorizationHeader
    ) {
        AuthenticatedUser user = jwtService.authenticate(authorizationHeader);
        if (request == null || request.cardNumber() == null
                || request.cardHolder() == null || request.cardHolder().isBlank()
                || request.expiry() == null || request.cvv() == null) {
            throw new WebApplicationException("Complete all fake card fields", Response.Status.BAD_REQUEST);
        }

        String cardNumber = request.cardNumber().replaceAll("\\s", "");
        if (!cardNumber.matches("\\d{16}") || !request.cvv().matches("\\d{3}")) {
            throw new WebApplicationException("Invalid demo card number or CVV", Response.Status.BAD_REQUEST);
        }
        try {
            if (LocalDate.parse(request.expiry()).isBefore(LocalDate.now())) {
                throw new WebApplicationException("The demo card is expired", Response.Status.BAD_REQUEST);
            }
        } catch (DateTimeParseException exception) {
            throw new WebApplicationException("Invalid demo card expiry date", Response.Status.BAD_REQUEST);
        }

        var normalizedRequest = new CardRegistrationRequest(
                cardNumber,
                request.cardHolder().trim(),
                request.expiry(),
                request.cvv(),
                request.reservationRequestId());
        var account = bankingClient.registerAccount(user.userId(), user.username(), normalizedRequest);
        if (!account.connected() || account.accountId() == null || account.accountId().isBlank()) {
            throw new WebApplicationException("Banking service returned an invalid account", Response.Status.BAD_GATEWAY);
        }

        if (request.reservationRequestId() != null && !request.reservationRequestId().isBlank()) {
            publishBankLogin(request.reservationRequestId(), user, account.accountId());
        }

        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @POST
    @Path("/login")
    public Response login(
            BankLoginRequest request,
            @HeaderParam("Authorization") String authorizationHeader
    ) {
        AuthenticatedUser user = jwtService.authenticate(authorizationHeader);
        if (request == null || request.reservationRequestId() == null
                || request.reservationRequestId().isBlank()) {
            throw new WebApplicationException("Reservation request id is required", Response.Status.BAD_REQUEST);
        }

        var account = bankingClient.getAccount(user.userId());
        if (!account.connected() || account.accountId() == null || account.accountId().isBlank()) {
            throw new WebApplicationException("Connect a bank account first", Response.Status.CONFLICT);
        }

        publishBankLogin(request.reservationRequestId(), user, account.accountId());
        return Response.ok(account).build();
    }

    @POST
    @Path("/recharge")
    public Response recharge(
            RechargeRequest request,
            @HeaderParam("Authorization") String authorizationHeader
    ) {
        AuthenticatedUser user = jwtService.authenticate(authorizationHeader);
        if (request.amount() <= 0 || request.amount() > 500) {
            throw new WebApplicationException("Recharge must be between 0 and 500", Response.Status.BAD_REQUEST);
        }
        var account = bankingClient.recharge(user.userId(), request.amount());

        if (request.reservationId() != null && !request.reservationId().isBlank()) {
            camundaClient.newPublishMessageCommand()
                    .messageName("ACCOUNT_RECHARGED")
                    .correlationKey(request.reservationId())
                    .variables(Map.of("rechargedBalance", account.balance()))
                    .send()
                    .join();
        }

        return Response.ok(account).build();
    }

    private void publishBankLogin(
            String reservationRequestId,
            AuthenticatedUser user,
            String accountId
    ) {
        pendingBankConnections.requireNotOwnedByAnotherUser(reservationRequestId, user.userId());
        try {
            camundaClient.newPublishMessageCommand()
                    .messageName("BANK_LOGIN_DATA")
                    .correlationKey(reservationRequestId)
                    .timeToLive(java.time.Duration.ofMinutes(10))
                    .variables(Map.of("bankLoginPayload", Map.of(
                            "username", user.username(),
                            "accountId", accountId)))
                    .send()
                    .join();
        } catch (RuntimeException exception) {
            throw new WebApplicationException(
                    "Unable to resume the pending reservation process",
                    exception,
                    Response.Status.SERVICE_UNAVAILABLE);
        }
        pendingBankConnections.consume(reservationRequestId, user.userId());
    }
}
