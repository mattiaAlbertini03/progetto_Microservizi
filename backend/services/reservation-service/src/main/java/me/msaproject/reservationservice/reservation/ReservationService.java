package me.msaproject.reservationservice.reservation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.reservationservice.client.LogisticsClient;
import me.msaproject.reservationservice.db.ReservationRepository;
import me.msaproject.reservationservice.dto.ReservationEvent;
import me.msaproject.reservationservice.dto.ReservationCreationRequest;
import me.msaproject.reservationservice.dto.ReservationResponse;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final LogisticsClient logisticsClient;

    @Inject
    public ReservationService(
            ReservationRepository reservationRepository,
            LogisticsClient logisticsClient) {
        this.reservationRepository = reservationRepository;
        this.logisticsClient = logisticsClient;
    }

    public Reservation createReservation(ReservationCreationRequest request) {
        String reservationId = UUID.randomUUID().toString();

        Reservation reservation = new Reservation(
                reservationId,
                request.userId(),
                request.vehicleId(),
                request.stationId(),
                "PENDING");

        reservationRepository.getReservations().put(reservationId, reservation);
        reservationRepository.sync();

        return reservation;
    }

    public ReservationEvent holdReservation(String reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"PENDING".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be held from status: " + reservation.status());
        }

        logisticsClient.holdVehicle(reservation.vehicleId());

        updateReservationStatus(reservation, "RESERVED");

        return new ReservationEvent(
                reservationId,
                "RESERVED",
                "Veicolo bloccato per la prenotazione");
    }

    public ReservationEvent pickupReservation(String reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"RESERVED".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be picked up from status: " + reservation.status());
        }

        logisticsClient.pickupVehicle(reservation.vehicleId());

        updateReservationStatus(reservation, "PICKED_UP");

        return new ReservationEvent(
                reservationId,
                "PICKED_UP",
                "Veicolo ritirato con successo");
    }

    public ReservationEvent cancelReservation(String reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"PENDING".equals(reservation.status()) &&
                !"RESERVED".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be cancelled from status: " + reservation.status());
        }

        logisticsClient.releaseVehicle(reservation.vehicleId());

        updateReservationStatus(reservation, "CANCELLED");

        return new ReservationEvent(
                reservationId,
                "CANCELLED",
                "Prenotazione annullata");
    }

    public ReservationEvent endReservation(String reservationId) {

        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"ACTIVE".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be ended from status: " + reservation.status());
        }

        logisticsClient.returnVehicle(reservation.vehicleId());

        updateReservationStatus(reservation, "COMPLETED");

        return new ReservationEvent(
                reservationId,
                "COMPLETED",
                "Noleggio terminato");
    }

    public Reservation getReservation(String reservationId) {
        return findReservationOrThrow(reservationId);
    }

    private Reservation findReservationOrThrow(String reservationId) {
        Reservation reservation = reservationRepository
                .getReservations()
                .get(reservationId);

        if (reservation == null) {
            throw new IllegalArgumentException("Reservation not found: " + reservationId);
        }

        return reservation;
    }

    private void updateReservationStatus(Reservation old, String status) {
        Reservation updated = new Reservation(
                old.reservationId(),
                old.userId(),
                old.vehicleId(),
                old.stationId(),
                status);

        reservationRepository.getReservations().put(old.reservationId(), updated);
        reservationRepository.sync();
    }

    public List<ReservationResponse> getReservationsByUserId(String userId) {

        return reservationRepository.getReservations()
                .values()
                .stream()
                .filter(reservation -> reservation.userId().equals(userId))
                .map(reservation -> new ReservationResponse(
                        reservation.reservationId(),
                        reservation.userId(),
                        reservation.vehicleId(),
                        reservation.stationId(),
                        reservation.status()))
                .toList();
    }
}