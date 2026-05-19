package me.msaproject.reservationservice.db;

import java.util.Map;

import me.msaproject.reservationservice.reservation.Reservation;

public interface ReservationRepository {

    Map<String, Reservation> getReservations();

    void sync();
}