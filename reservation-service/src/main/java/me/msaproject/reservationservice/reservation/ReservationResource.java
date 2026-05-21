package me.msaproject.reservationservice.reservation;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.msaproject.reservationservice.dto.ReservationEvent;
import me.msaproject.reservationservice.dto.ReservationRequest;
import me.msaproject.reservationservice.dto.ReservationResponse;
import me.msaproject.reservationservice.dto.ReservationStatusResponse;

@Path("/reservations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReservationResource {

        @Inject
        ReservationService reservationService;

        @GET
        @Path("/{reservationId}")
        public Response getReservation(
                        @PathParam("reservationId") String reservationId) {

                Reservation reservation = reservationService.getReservation(reservationId);

                return Response.ok(new ReservationResponse(
                                reservation.id(),
                                reservation.userId(),
                                reservation.vehicleId(),
                                reservation.stationId(),
                                reservation.status())).build();
        }

        @POST
        public Response createReservation(ReservationRequest request) {
                Reservation reservation = reservationService.createReservation(request);

                return Response
                                .status(Response.Status.CREATED)
                                .entity(new ReservationResponse(
                                                reservation.id(),
                                                reservation.userId(),
                                                reservation.vehicleId(),
                                                reservation.stationId(),
                                                reservation.status()))
                                .build();
        }

        @POST
        @Path("/{reservationId}/hold")
        public Response holdReservation(
                        @PathParam("reservationId") String reservationId) {

                ReservationEvent event = reservationService.holdReservation(reservationId);

                return Response.ok(new ReservationStatusResponse(
                                event.reservationId(),
                                event.status())).build();
        }

        @POST
        @Path("/{reservationId}/pickup")
        public Response pickupReservation(
                        @PathParam("reservationId") String reservationId) {

                ReservationEvent event = reservationService.pickupReservation(reservationId);

                return Response.ok(new ReservationStatusResponse(
                                event.reservationId(),
                                event.status())).build();
        }

        @POST
        @Path("/{reservationId}/cancel")
        public Response cancelReservation(
                        @PathParam("reservationId") String reservationId) {

                ReservationEvent event = reservationService.cancelReservation(reservationId);

                return Response.ok(new ReservationStatusResponse(
                                event.reservationId(),
                                event.status())).build();
        }

        @POST
        @Path("/{reservationId}/end")
        public Response endReservation(
                        @PathParam("reservationId") String reservationId) {

                ReservationEvent event = reservationService.endReservation(reservationId);

                return Response.ok(new ReservationStatusResponse(
                                event.reservationId(),
                                event.status())).build();
        }

        @GET
        public Response getReservations(
                        @QueryParam("userId") String userId) {

                if (userId == null || userId.isBlank()) {
                        throw new WebApplicationException(
                                        "Missing userId",
                                        Response.Status.BAD_REQUEST);
                }

                List<ReservationResponse> reservations = reservationService.getReservationsByUserId(userId);

                return Response.ok(reservations).build();
        }
}