package me.msaproject.acmemobilityservice.api;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.FleetClient;
import me.msaproject.acmemobilityservice.client.ReservationsClient;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequest;
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
                        ReservationRequest request,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = extractUserId(authorizationHeader);

                if (!userId.equals(request.userId())) {
                        throw new WebApplicationException("User mismatch", Response.Status.FORBIDDEN);
                }

                String requestId = UUID.randomUUID().toString();
                ReservationRequest createRequest = new ReservationRequest(
                                requestId,
                                userId,
                                request.vehicleId(),
                                request.stationId());

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("ONLINE_RESERVATION_REQUEST")
                                .correlationKey(requestId)
                                .variables(Map.of("reservationRequest", createRequest))
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
                        ReservationRequest request,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = extractUserId(authorizationHeader);

                if (!userId.equals(request.userId())) {
                        throw new WebApplicationException("User mismatch", Response.Status.FORBIDDEN);
                }

                String requestId = UUID.randomUUID().toString();

                ReservationRequest qrPickupRequest = new ReservationRequest(
                                requestId,
                                userId,
                                request.vehicleId(),
                                request.stationId());

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("QR_PICKUP_REQUEST")
                                .correlationKey(requestId)
                                .variables(Map.of("reservationRequest", qrPickupRequest))
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

                ReservationResponse reservation = reservationClient.getReservation(reservationId);

                if (!reservation.userId().equals(userId)) {
                        throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
                }

                ReservationRequest cancelRequest = new ReservationRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId());

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("CANCEL_RESERVATION_REQUEST")
                                .correlationKey(reservationId)
                                .variables(Map.of("reservationRequest", cancelRequest))
                                .send()
                                .join();

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

                ReservationResponse reservation = reservationClient.getReservation(reservationId);

                if (!reservation.userId().equals(userId)) {
                        throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
                }

                ReservationRequest pickupRequest = new ReservationRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId());

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("PICKUP_RESERVATION_REQUEST")
                                .correlationKey(reservationId)
                                .variables(Map.of("reservationRequest", pickupRequest))
                                .send()
                                .join();

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

                ReservationResponse reservation = reservationClient.getReservation(reservationId);

                if (!reservation.userId().equals(userId)) {
                        throw new WebApplicationException("Forbidden", Response.Status.FORBIDDEN);
                }

                if (!"ACTIVE".equals(reservation.status())) {
                        throw new WebApplicationException(
                                        "Reservation cannot be returned from status: " + reservation.status(),
                                        Response.Status.CONFLICT);
                }

                boolean vehicleAtDestination = fleetClient.isVehicleAtDestination(reservationId);

                if (!vehicleAtDestination) {
                        throw new WebApplicationException(
                                        "Vehicle has not reached the destination station yet",
                                        Response.Status.CONFLICT);
                }

                ReservationRequest returnRequest = new ReservationRequest(
                                reservationId,
                                userId,
                                reservation.vehicleId(),
                                reservation.stationId());

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("RESERVATION_RETURN_REQUEST")
                                .correlationKey(reservationId)
                                .variables(Map.of("reservationRequest", returnRequest))
                                .send()
                                .join();

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

                List<ReservationResponse> reservations = reservationClient.getReservationsByUserId(userId);

                return Response.ok(reservations).build();
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