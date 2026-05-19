package me.msaproject.acmemobilityservice.api;

import io.camunda.client.CamundaClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.acmemobilityservice.client.ReservationClient;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequest;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationRequestStatusResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationResponse;
import me.msaproject.acmemobilityservice.dto.reservation.ReservationStatusResponse;

import java.util.Map;
import java.util.UUID;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationApi {

        @Inject
        CamundaClient camundaClient;

        @Inject
        ReservationClient reservationClient;

        @POST
        public Response createReservation(
                        ReservationRequest request,
                        @HeaderParam("Authorization") String authorizationHeader) {

                String userId = extractUserId(authorizationHeader);

                if (!userId.equals(request.userId())) {
                        throw new WebApplicationException("User mismatch", Response.Status.FORBIDDEN);
                }

                String requestId = UUID.randomUUID().toString();

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("ONLINE_RESERVATION_REQUEST")
                                .correlationKey(requestId)
                                .variables(Map.of(
                                                "requestId", requestId,
                                                "userId", userId,
                                                "vehicleId", request.vehicleId(),
                                                "stationId", request.stationId()))
                                .send()
                                .join();

                return Response.accepted(new ReservationRequestStatusResponse(
                                requestId,
                                userId,
                                "CREATION_PENDING")).build();
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

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("CANCEL_RESERVATION_REQUEST")
                                .correlationKey(reservationId)
                                .variables(Map.of(
                                                "reservationId", reservationId,
                                                "userId", userId))
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

                camundaClient
                                .newPublishMessageCommand()
                                .messageName("PICKUP_RESERVATION_REQUEST")
                                .correlationKey(reservationId)
                                .variables(Map.of(
                                                "reservationId", reservationId,
                                                "userId", userId))
                                .send()
                                .join();

                return Response.accepted(new ReservationStatusResponse(
                                reservationId,
                                "PICKUP_PENDING")).build();
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