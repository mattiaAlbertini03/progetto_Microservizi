package me.msaproject.acmemobilityservice.reservation;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ReservationService {

    public String createReservation(ReservationRequest request) {
        return UUID.randomUUID().toString();
    }
}