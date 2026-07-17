package me.msaproject.reservationservice.db;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import me.msaproject.reservationservice.reservation.Reservation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Alternative
@Priority(1)
@ApplicationScoped
public class InMemoryReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(reservations.values());
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.reservationId(), reservation);
    }

    @Override
    public void deleteById(String reservationId) {
        reservations.remove(reservationId);
    }
}
