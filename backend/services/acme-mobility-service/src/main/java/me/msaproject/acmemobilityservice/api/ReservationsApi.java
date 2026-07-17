package me.msaproject.acmemobilityservice.api;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.LogisticsClient;
import me.msaproject.acmemobilityservice.client.BankingClient;
import me.msaproject.acmemobilityservice.client.ReservationsClient;
import me.msaproject.acmemobilityservice.dto.reservation.QrPickupRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationCreationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationActionRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequestStatusResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationStatusResponse;
import me.msaproject.acmemobilityservice.auth.AuthenticatedUser;
import me.msaproject.acmemobilityservice.auth.JwtService;
import me.msaproject.acmemobilityservice.auth.PendingBankConnectionService;
import me.msaproject.acmemobilityservice.dto.bank.BankAccountResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationsApi {

        private static final System.Logger LOGGER = System.getLogger(ReservationsApi.class.getName());
        private static final Duration MAX_RESERVATION_ADVANCE = Duration.ofMinutes(30);
        private static final Duration SCHEDULE_CLOCK_TOLERANCE = Duration.ofMinutes(1);
        private static final Duration FREE_CANCELLATION_MARGIN = Duration.ofMinutes(5);
        private static final Duration PICKUP_GRACE_PERIOD = Duration.ofMinutes(5);

        @Inject
        CamundaClient camundaClient;

        @Inject
        ReservationsClient reservationClient;

        @Inject
        LogisticsClient logisticsClient;

        @Inject
        BankingClient bankingClient;

        @Inject
        PendingBankConnectionService pendingBankConnections;

        @Inject
        JwtService jwtService;

        @POST
        public Response createReservation(
                        ReservationCreationRequest request,
                        @HeaderParam("Authorization") String authorizationHeader) {

                AuthenticatedUser authenticatedUser = jwtService.authenticate(authorizationHeader);

                String userId = authenticatedUser.userId();
                String username = authenticatedUser.username();

                if (!userId.equals(request.userId())) {
                        return Response.status(Response.Status.FORBIDDEN)
                                        .entity(Map.of(
                                                        "status", "FORBIDDEN",
                                                        "message",
                                                        "The authenticated user does not match the requested user"))
                                        .build();
                }

                String requestId = UUID.randomUUID().toString();
                Instant scheduledStart = validateScheduledStart(request.scheduledStartEpochMs());

                ReservationCreationRequest createRequest = new ReservationCreationRequest(
                                requestId,
                                userId,
                                request.vehicleId(),
                                request.stationId(),
                                scheduledStart.toEpochMilli());

                BankAccountResponse bankAccount = bankingClient.getAccount(userId);
                if (!bankAccount.connected()) {
                        pendingBankConnections.register(requestId, userId);
                }
                Map<String, Object> bankLoginPayload = Map.of(
                                "username", username,
                                "accountId", bankAccount.connected() ? bankAccount.accountId() : userId);

                try {
                        startReservationProcess(
                                        createRequest,
                                        "ONLINE",
                                        bankLoginPayload,
                                        bankAccount.bankSessionId());

                } catch (Exception exception) {
                        pendingBankConnections.remove(requestId);
                        LOGGER.log(System.Logger.Level.ERROR,
                                        "Unable to start reservation process " + requestId,
                                        exception);

                        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                        .entity(Map.of(
                                                        "requestId", requestId,
                                                        "userId", userId,
                                                        "status", "CAMUNDA_UNAVAILABLE",
                                                        "message", "Unable to start the reservation process"))
                                        .build();
                }

                return Response.accepted(
                                new ReservationRequestStatusResponse(
                                                requestId,
                                                userId,
                                                "CREATION_PENDING"))
                                .build();
        }

        @POST
        @Path("/qr-pickup")
        public Response createQrReservation(
                        QrPickupRequest request,
                        @HeaderParam("Authorization") String authorizationHeader) {

                AuthenticatedUser authenticatedUser = jwtService.authenticate(authorizationHeader);

                String userId = authenticatedUser.userId();
                String username = authenticatedUser.username();

                if (!userId.equals(request.userId())) {
                        return Response.status(Response.Status.FORBIDDEN)
                                        .entity(Map.of(
                                                        "status", "FORBIDDEN",
                                                        "message",
                                                        "The authenticated user does not match the requested user"))
                                        .build();
                }

                String expectedQrCode = logisticsClient
                                .getVehicle(request.vehicleId())
                                .qrCode();
                if (request.qrCode() == null || !request.qrCode().equals(expectedQrCode)) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Map.of(
                                                        "status", "INVALID_QR_CODE",
                                                        "message", "The QR code does not match the selected vehicle"))
                                        .build();
                }

                String requestId = UUID.randomUUID().toString();
                Instant scheduledStart = Instant.now();

                ReservationCreationRequest qrPickupRequest = new ReservationCreationRequest(
                                requestId,
                                userId,
                                request.vehicleId(),
                                request.stationId(),
                                scheduledStart.toEpochMilli());

                BankAccountResponse bankAccount = bankingClient.getAccount(userId);
                if (!bankAccount.connected()) {
                        pendingBankConnections.register(requestId, userId);
                }
                Map<String, Object> bankLoginPayload = Map.of(
                                "username", username,
                                "accountId", bankAccount.connected() ? bankAccount.accountId() : userId);

                try {
                        startReservationProcess(
                                        qrPickupRequest,
                                        "QR_PICKUP",
                                        bankLoginPayload,
                                        bankAccount.bankSessionId());

                } catch (Exception exception) {
                        pendingBankConnections.remove(requestId);
                        LOGGER.log(System.Logger.Level.ERROR,
                                        "Unable to start QR pickup process " + requestId,
                                        exception);

                        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                                        .entity(Map.of(
                                                        "requestId", requestId,
                                                        "userId", userId,
                                                        "status", "CAMUNDA_UNAVAILABLE",
                                                        "message", "Unable to start the QR pickup process"))
                                        .build();
                }

                return Response.accepted(
                                new ReservationRequestStatusResponse(
                                                requestId,
                                                userId,
                                                "PICKUP_PENDING"))
                                .build();
        }

        private void startReservationProcess(
                        ReservationCreationRequest request,
                        String reservationType,
                        Map<String, Object> bankLoginPayload,
                        String bankSessionId) {

                String vehicleName = logisticsClient.getVehicle(request.vehicleId()).name();

                Map<String, Object> variables = new HashMap<>(Map.of(
                                "reservationRequest", request,
                                "reservationType", reservationType,
                                "bankLoginPayload", bankLoginPayload,
                                "username", bankLoginPayload.get("username"),
                                "vehicleName", vehicleName,
                                "scheduledStartAt", Instant
                                                .ofEpochMilli(request.scheduledStartEpochMs())
                                                .toString(),
                                "pickupDeadlineAt", Instant
                                                .ofEpochMilli(request.scheduledStartEpochMs())
                                                .plus(PICKUP_GRACE_PERIOD)
                                                .toString()));
                if (bankSessionId != null && !bankSessionId.isBlank()) {
                        variables.put("bankSessionId", bankSessionId);
                }

                camundaClient
                                .newCreateInstanceCommand()
                                .bpmnProcessId("process")
                                .latestVersion()
                                .variables(variables)
                                .send()
                                .join();
        }

        @POST
        @Path("/{reservationId}/cancel")
        public Response cancelReservation(
                        @PathParam("reservationId") String reservationId,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = jwtService
                                .authenticate(authorizationHeader)
                                .userId();

                ReservationResponse reservation = getOwnedReservation(reservationId, userId);
                boolean lateCancellation = Instant.now().isAfter(
                                Instant.ofEpochMilli(reservation.scheduledStartEpochMs())
                                                .minus(FREE_CANCELLATION_MARGIN));

                ReservationActionRequest cancelRequest = new ReservationActionRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId(),
                                lateCancellation);

                publishReservationAction(
                                lateCancellation
                                                ? "LATE_CANCEL_RESERVATION_REQUEST"
                                                : "CANCEL_RESERVATION_REQUEST",
                                reservationId,
                                cancelRequest);

                return Response.accepted(new ReservationStatusResponse(
                                reservationId,
                                "CANCELLATION_PENDING")).build();
        }

        @POST
        @Path("/{reservationId}/pickup")
        public Response pickupReservation(
                        @PathParam("reservationId") String reservationId,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = jwtService
                                .authenticate(authorizationHeader)
                                .userId();

                ReservationResponse reservation = getOwnedReservation(reservationId, userId);

                ReservationActionRequest pickupRequest = new ReservationActionRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId(),
                                false);

                publishReservationAction(
                                "PICKUP_RESERVATION_REQUEST",
                                reservationId,
                                pickupRequest);

                return Response.accepted(new ReservationStatusResponse(
                                reservationId,
                                "PICKUP_PENDING")).build();
        }

        @POST
        @Path("/{reservationId}/return")
        public Response returnReservation(
                        @PathParam("reservationId") String reservationId,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = jwtService
                                .authenticate(authorizationHeader)
                                .userId();

                ReservationResponse reservation = getOwnedReservation(reservationId, userId);

                if (!"ACTIVE".equals(reservation.status()) &&
                                !"PICKED_UP".equals(reservation.status())) {
                        throw new WebApplicationException(
                                        "Reservation cannot be returned from status: " + reservation.status(),
                                        Response.Status.CONFLICT);
                }

                ReservationActionRequest returnRequest = new ReservationActionRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId(),
                                false);

                publishReservationAction(
                                "RESERVATION_RETURN_REQUEST",
                                reservationId,
                                returnRequest);

                return Response.accepted(new ReservationStatusResponse(
                                reservationId,
                                "RETURN_PENDING")).build();
        }

        @GET
        public Response getReservationsForCurrentUser(
                        @QueryParam("userId") String userId,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String authenticatedUserId = jwtService
                                .authenticate(authorizationHeader)
                                .userId();

                if (!authenticatedUserId.equals(userId)) {
                        throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
                }

                List<ReservationResponse> reservations = reservationClient.getReservationsByUserId(userId);

                return Response.ok(reservations).build();
        }

        private ReservationResponse getOwnedReservation(String reservationId, String userId) {
                ReservationResponse reservation = reservationClient.getReservation(reservationId);

                if (!reservation.userId().equals(userId)) {
                        throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
                }

                return reservation;
        }

        private Instant validateScheduledStart(long scheduledStartEpochMs) {
                Instant now = Instant.now();
                Instant scheduledStart = Instant.ofEpochMilli(scheduledStartEpochMs);

                if (scheduledStart.isBefore(now.minus(SCHEDULE_CLOCK_TOLERANCE)) ||
                                scheduledStart.isAfter(now.plus(MAX_RESERVATION_ADVANCE))) {
                        throw new WebApplicationException(
                                        "Reservation start must be between now and 30 minutes from now",
                                        Response.Status.BAD_REQUEST);
                }

                return scheduledStart;
        }

        private void publishReservationAction(
                        String messageName,
                        String reservationId,
                        ReservationActionRequest actionRequest) {

                try {
                        camundaClient
                                        .newPublishMessageCommand()
                                        .messageName(messageName)
                                        .correlationKey(reservationId)
                                        .variables(Map.of("reservationAction", actionRequest))
                                        .send()
                                        .join();
                } catch (Exception e) {
                        throw new WebApplicationException(
                                        "No active process found for reservation " + reservationId,
                                        Response.Status.CONFLICT);
                }
        }
}
