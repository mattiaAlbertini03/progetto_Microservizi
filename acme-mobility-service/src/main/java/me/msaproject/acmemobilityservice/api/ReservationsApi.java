package me.msaproject.acmemobilityservice.api;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.FleetClient;
import me.msaproject.acmemobilityservice.client.ReservationsClient;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationCreationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationActionRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequestStatusResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationStatusResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationsApi {

    @Inject
    CamundaClient camundaClient;

    @Inject
    ReservationsClient reservationClient;

    @Inject
    FleetClient fleetClient;

    @POST
    public Response createReservation(
            ReservationCreationRequest request,
            @HeaderParam("Authorization") String authorizationHeader) {

        String userId = extractUserId(authorizationHeader);

        if (!userId.equals(request.userId())) {
            throw new WebApplicationException("User mismatch", Response.Status.FORBIDDEN);
        }

        String requestId = UUID.randomUUID().toString();

        ReservationCreationRequest createRequest = new ReservationCreationRequest(
                requestId,
                userId,
                request.vehicleId(),
                request.stationId());

        camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId("process")
                .latestVersion()
                .variables(Map.of(
                        "reservationRequest", createRequest,
                        "reservationType", "ONLINE"))
                .send()
                .join();

        return Response.accepted(new ReservationRequestStatusResponse(
                requestId,
                userId,
                "CREATION_PENDING")).build();
    }

    @POST
    @Path("/qr-pickup")
    public Response createQrReservation(
            ReservationCreationRequest request,
            @HeaderParam("Authorization") String authorizationHeader) {

        String userId = extractUserId(authorizationHeader);

        if (!userId.equals(request.userId())) {
            throw new WebApplicationException("User mismatch", Response.Status.FORBIDDEN);
        }

        String requestId = UUID.randomUUID().toString();

        ReservationCreationRequest qrPickupRequest = new ReservationCreationRequest(
                requestId,
                userId,
                request.vehicleId(),
                request.stationId());

        camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId("process")
                .latestVersion()
                .variables(Map.of(
                        "reservationRequest", qrPickupRequest,
                        "reservationType", "QR_PICKUP"))
                .send()
                .join();

        return Response.accepted(new ReservationRequestStatusResponse(
                requestId,
                userId,
                "PICKUP_PENDING")).build();
    }

    @POST
    @Path("/{reservationId}/cancel")
    public Response cancelReservation(
            @PathParam("reservationId") String reservationId,
            @HeaderParam("Authorization") String authorizationHeader) {

        String userId = extractUserId(authorizationHeader);
        ReservationResponse reservation = getOwnedReservation(reservationId, userId);

        ReservationActionRequest cancelRequest = new ReservationActionRequest(
                reservationId,
                userId,
                reservation.vehicleId(),
                reservation.stationId());

        publishReservationAction(
                "CANCEL_RESERVATION_REQUEST",
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

        String userId = extractUserId(authorizationHeader);
        ReservationResponse reservation = getOwnedReservation(reservationId, userId);

        ReservationActionRequest pickupRequest = new ReservationActionRequest(
                reservationId,
                userId,
                reservation.vehicleId(),
                reservation.stationId());

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

        String userId = extractUserId(authorizationHeader);
        ReservationResponse reservation = getOwnedReservation(reservationId, userId);

        if (!"ACTIVE".equals(reservation.status())) {
            throw new WebApplicationException(
                    "Reservation cannot be returned from status: " + reservation.status(),
                    Response.Status.CONFLICT);
        }

        ReservationActionRequest returnRequest = new ReservationActionRequest(
                reservationId,
                userId,
                reservation.vehicleId(),
                reservation.stationId());

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

        String authenticatedUserId = extractUserId(authorizationHeader);

        if (!authenticatedUserId.equals(userId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        List<ReservationResponse> reservations =
                reservationClient.getReservationsByUserId(userId);

        return Response.ok(reservations).build();
    }

    private ReservationResponse getOwnedReservation(String reservationId, String userId) {
        ReservationResponse reservation = reservationClient.getReservation(reservationId);

        if (!reservation.userId().equals(userId)) {
            throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
        }

        return reservation;
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

    private String extractUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new WebApplicationException(
                    "Missing authorization token",
                    Response.Status.UNAUTHORIZED);
        }

        return authorizationHeader.substring("Bearer ".length());
    }
}