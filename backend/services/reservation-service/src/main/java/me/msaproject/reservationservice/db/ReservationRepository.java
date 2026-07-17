package me.msaproject.reservationservice.db;

import me.msaproject.reservationservice.reservation.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    List<Reservation> findAll();

    Optional<Reservation> findById(String reservationId);

    void save(Reservation reservation);

    void deleteById(String reservationId);
}