package me.msaproject.reservationservice.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import me.msaproject.reservationservice.reservation.Reservation;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class JsonReservationRepository implements ReservationRepository {

    private final Map<String, Reservation> reservations;

    public JsonReservationRepository() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("reservations.json");

            if (inputStream == null) {
                this.reservations = new ConcurrentHashMap<>();
                return;
            }

            List<Reservation> loadedReservations = mapper.readValue(
                    inputStream,
                    new TypeReference<List<Reservation>>() {}
            );

            this.reservations = new ConcurrentHashMap<>();

            for (Reservation reservation : loadedReservations) {
                reservations.put(reservation.id(), reservation);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load reservations.json", e);
        }
    }

    @Override
    public Map<String, Reservation> getReservations() {
        return reservations;
    }

    @Override
    public void sync() {
        // TODO:
        // qui in futuro sincronizzerai reservations con un database reale.
    }
}