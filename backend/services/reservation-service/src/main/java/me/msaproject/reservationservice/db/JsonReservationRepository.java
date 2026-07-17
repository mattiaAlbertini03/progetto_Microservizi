package me.msaproject.reservationservice.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import me.msaproject.reservationservice.reservation.Reservation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class JsonReservationRepository implements ReservationRepository {

    private static final Path DATA_FILE = Path.of(
            System.getenv().getOrDefault("RESERVATIONS_DATA_FILE", "/data/reservations.json"));

    private final Map<String, Reservation> reservations;
    private final ObjectMapper mapper;

    public JsonReservationRepository() {
        try {
            mapper = new ObjectMapper();

            if (Files.exists(DATA_FILE)) {
                this.reservations = loadReservations(Files.newInputStream(DATA_FILE));
                return;
            }

            InputStream inputStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("reservations.json");

            if (inputStream == null) {
                this.reservations = new ConcurrentHashMap<>();
                return;
            }

            this.reservations = loadReservations(inputStream);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load reservations.json", e);
        }
    }

    @Override
    public Map<String, Reservation> getReservations() {
        return reservations;
    }

    @Override
    public synchronized void sync() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            Path temporaryFile = DATA_FILE.resolveSibling(DATA_FILE.getFileName() + ".tmp");
            mapper.writeValue(temporaryFile.toFile(), reservations.values());
            try {
                Files.move(temporaryFile, DATA_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist reservations", e);
        }
    }

    private Map<String, Reservation> loadReservations(InputStream inputStream) throws Exception {
        try (inputStream) {
            List<Reservation> loadedReservations = mapper.readValue(
                    inputStream,
                    new TypeReference<List<Reservation>>() {});
            Map<String, Reservation> loaded = new ConcurrentHashMap<>();
            for (Reservation reservation : loadedReservations) {
                loaded.put(reservation.reservationId(), reservation);
            }
            return loaded;
        }
    }
}
