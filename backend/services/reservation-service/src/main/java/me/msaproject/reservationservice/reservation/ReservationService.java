package me.msaproject.reservationservice.reservation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.reservationservice.client.LogisticsClient;
import me.msaproject.reservationservice.db.ReservationRepository;
import me.msaproject.reservationservice.dto.ReservationEvent;
import me.msaproject.reservationservice.dto.ReservationCreationRequest;
import me.msaproject.reservationservice.dto.ReservationResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReservationService {

    private static final Duration MAX_RESERVATION_ADVANCE = Duration.ofMinutes(30);
    private static final Duration SCHEDULE_CLOCK_TOLERANCE = Duration.ofMinutes(1);
    private static final Duration PICKUP_GRACE_PERIOD = Duration.ofMinutes(5);

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
        Instant now = Instant.now();
        Instant scheduledStart = Instant.ofEpochMilli(request.scheduledStartEpochMs());

        if (scheduledStart.isBefore(now.minus(SCHEDULE_CLOCK_TOLERANCE)) ||
                scheduledStart.isAfter(now.plus(MAX_RESERVATION_ADVANCE))) {
            throw new IllegalArgumentException(
                    "scheduledStartEpochMs must be between now and 30 minutes from now");
        }

        String reservationId = UUID.randomUUID().toString();

        Reservation reservation = new Reservation(
                reservationId,
                request.userId(),
                request.vehicleId(),
                request.stationId(),
                "PENDING",
                now.toEpochMilli(),
                scheduledStart.toEpochMilli(),
                scheduledStart.plus(PICKUP_GRACE_PERIOD).toEpochMilli());

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

        if (!"RESERVED".equals(reservation.status()) &&
                !"PENDING".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be picked up from status: " + reservation.status());
        }

        if ("PENDING".equals(reservation.status())) {
            logisticsClient.holdVehicle(reservation.vehicleId());
        }

        logisticsClient.pickupVehicle(reservation.vehicleId());

        updateReservationStatus(reservation, "ACTIVE");

        return new ReservationEvent(
                reservationId,
                "ACTIVE",
                "Veicolo ritirato con successo");
    }

    public ReservationEvent cancelReservation(String reservationId) {
        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"PENDING".equals(reservation.status()) &&
                !"RESERVED".equals(reservation.status())) {
            throw new IllegalStateException(
                    "Reservation cannot be cancelled from status: " + reservation.status());
        }

        if ("RESERVED".equals(reservation.status())) {
            logisticsClient.releaseVehicle(reservation.vehicleId());
        }

        updateReservationStatus(reservation, "CANCELLED");

        return new ReservationEvent(
                reservationId,
                "CANCELLED",
                "Prenotazione annullata");
    }

    public ReservationEvent endReservation(String reservationId) {

        Reservation reservation = findReservationOrThrow(reservationId);

        if (!"ACTIVE".equals(reservation.status()) &&
                !"PICKED_UP".equals(reservation.status())) {
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
                status,
                old.createdAtEpochMs(),
                old.scheduledStartEpochMs(),
                old.pickupDeadlineEpochMs());

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
                        reservation.status(),
                        reservation.createdAtEpochMs(),
                        reservation.scheduledStartEpochMs(),
                        reservation.pickupDeadlineEpochMs()))
                .toList();
    }
}
