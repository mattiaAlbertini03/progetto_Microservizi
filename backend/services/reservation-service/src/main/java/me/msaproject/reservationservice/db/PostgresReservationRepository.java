package me.msaproject.reservationservice.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.reservationservice.reservation.Reservation;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresReservationRepository implements ReservationRepository {

    private static final String FIND_ALL_SQL = """
            SELECT
                reservation_id,
                user_id,
                vehicle_id,
                station_id,
                status,
                created_at_epoch_ms,
                scheduled_start_epoch_ms,
                pickup_deadline_epoch_ms,
                total_kilometers,
                payment_amount,
                completed_at_epoch_ms
            FROM reservations
            ORDER BY created_at_epoch_ms DESC
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT
                reservation_id,
                user_id,
                vehicle_id,
                station_id,
                status,
                created_at_epoch_ms,
                scheduled_start_epoch_ms,
                pickup_deadline_epoch_ms,
                total_kilometers,
                payment_amount,
                completed_at_epoch_ms
            FROM reservations
            WHERE reservation_id = ?
            """;

    private static final String SAVE_SQL = """
            INSERT INTO reservations (
                reservation_id,
                user_id,
                vehicle_id,
                station_id,
                status,
                created_at_epoch_ms,
                scheduled_start_epoch_ms,
                pickup_deadline_epoch_ms,
                total_kilometers,
                payment_amount,
                completed_at_epoch_ms
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (reservation_id)
            DO UPDATE SET
                user_id = EXCLUDED.user_id,
                vehicle_id = EXCLUDED.vehicle_id,
                station_id = EXCLUDED.station_id,
                status = EXCLUDED.status,
                created_at_epoch_ms = EXCLUDED.created_at_epoch_ms,
                scheduled_start_epoch_ms =
                    EXCLUDED.scheduled_start_epoch_ms,
                pickup_deadline_epoch_ms =
                    EXCLUDED.pickup_deadline_epoch_ms,
                total_kilometers = EXCLUDED.total_kilometers,
                payment_amount = EXCLUDED.payment_amount,
                completed_at_epoch_ms =
                    EXCLUDED.completed_at_epoch_ms
            """;

    private static final String DELETE_BY_ID_SQL = """
            DELETE FROM reservations
            WHERE reservation_id = ?
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;

    private HikariDataSource dataSource;

    @Inject
    public PostgresReservationRepository(
            @ConfigProperty(name = "reservation-db.url")
            String jdbcUrl,

            @ConfigProperty(name = "reservation-db.user")
            String username,

            @ConfigProperty(name = "reservation-db.password")
            String password,

            @ConfigProperty(
                    name = "reservation-db.pool.maximum-size",
                    defaultValue = "4"
            )
            int maximumPoolSize
    ) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maximumPoolSize = maximumPoolSize;
    }

    @PostConstruct
    void initialize() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setPoolName("reservation-db-pool");
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);

        dataSource = new HikariDataSource(config);

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    @PreDestroy
    void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(FIND_ALL_SQL);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                reservations.add(mapReservation(resultSet));
            }

            return reservations;

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to retrieve reservations",
                    exception
            );
        }
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return Optional.empty();
        }

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(FIND_BY_ID_SQL)
        ) {
            statement.setString(1, reservationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapReservation(resultSet));
            }

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to retrieve reservation: " + reservationId,
                    exception
            );
        }
    }

    @Override
    public void save(Reservation reservation) {
        validateReservation(reservation);

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(SAVE_SQL)
        ) {
            statement.setString(1, reservation.reservationId());
            statement.setString(2, reservation.userId());
            statement.setString(3, reservation.vehicleId());
            statement.setString(4, reservation.stationId());
            statement.setString(5, reservation.status());
            statement.setLong(6, reservation.createdAtEpochMs());
            statement.setLong(7, reservation.scheduledStartEpochMs());
            statement.setLong(8, reservation.pickupDeadlineEpochMs());

            setNullableDouble(
                    statement,
                    9,
                    reservation.totalKilometers()
            );

            setNullableBigDecimal(
                    statement,
                    10,
                    reservation.paymentAmount()
            );

            setNullableLong(
                    statement,
                    11,
                    reservation.completedAtEpochMs()
            );

            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to save reservation: "
                            + reservation.reservationId(),
                    exception
            );
        }
    }

    @Override
    public void deleteById(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return;
        }

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(DELETE_BY_ID_SQL)
        ) {
            statement.setString(1, reservationId);
            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to delete reservation: " + reservationId,
                    exception
            );
        }
    }

    private Reservation mapReservation(
            ResultSet resultSet
    ) throws SQLException {

        return new Reservation(
                resultSet.getString("reservation_id"),
                resultSet.getString("user_id"),
                resultSet.getString("vehicle_id"),
                resultSet.getString("station_id"),
                resultSet.getString("status"),
                resultSet.getLong("created_at_epoch_ms"),
                resultSet.getLong("scheduled_start_epoch_ms"),
                resultSet.getLong("pickup_deadline_epoch_ms"),
                getNullableDouble(resultSet, "total_kilometers"),
                resultSet.getBigDecimal("payment_amount"),
                getNullableLong(resultSet, "completed_at_epoch_ms")
        );
    }

    private void validateReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "Reservation cannot be null"
            );
        }

        if (reservation.reservationId() == null
                || reservation.reservationId().isBlank()) {
            throw new IllegalArgumentException(
                    "Reservation ID cannot be null or blank"
            );
        }
    }

    private void setNullableDouble(
            PreparedStatement statement,
            int index,
            Double value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    private void setNullableBigDecimal(
            PreparedStatement statement,
            int index,
            BigDecimal value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.NUMERIC);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    private void setNullableLong(
            PreparedStatement statement,
            int index,
            Long value
    ) throws SQLException {

        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private Double getNullableDouble(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private Long getNullableLong(
            ResultSet resultSet,
            String column
    ) throws SQLException {

        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
