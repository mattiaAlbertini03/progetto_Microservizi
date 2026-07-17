package me.msaproject.logisticsservice.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.logisticsservice.dto.logistics.VehicleStatus;
import me.msaproject.logisticsservice.logistics.Vehicle;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostgresVehicleRepository implements VehicleRepository {

    private static final String COLUMNS = """
            id, name, type, lat, lon, status, battery_pct, qr_code
            """;
    private static final String FIND_ALL_SQL = "SELECT " + COLUMNS + " FROM vehicles ORDER BY id";
    private static final String FIND_BY_ID_SQL = "SELECT " + COLUMNS + " FROM vehicles WHERE id = ?";
    private static final String SAVE_SQL = """
            INSERT INTO vehicles (id, name, type, lat, lon, status, battery_pct, qr_code)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id)
            DO UPDATE SET
                name = EXCLUDED.name,
                type = EXCLUDED.type,
                lat = EXCLUDED.lat,
                lon = EXCLUDED.lon,
                status = EXCLUDED.status,
                battery_pct = EXCLUDED.battery_pct,
                qr_code = EXCLUDED.qr_code
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private HikariDataSource dataSource;

    @Inject
    public PostgresVehicleRepository(
            @ConfigProperty(name = "logistics-db.url") String jdbcUrl,
            @ConfigProperty(name = "logistics-db.user") String username,
            @ConfigProperty(name = "logistics-db.password") String password,
            @ConfigProperty(name = "logistics-db.pool.maximum-size", defaultValue = "4")
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
        config.setPoolName("logistics-db-pool");
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setValidationTimeout(5_000);
        dataSource = new HikariDataSource(config);

        Flyway.configure().dataSource(dataSource).load().migrate();
    }

    @PreDestroy
    void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public List<Vehicle> findAll() {
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                vehicles.add(mapVehicle(resultSet));
            }
            return vehicles;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to retrieve vehicles", exception);
        }
    }

    @Override
    public Optional<Vehicle> findById(String vehicleId) {
        if (vehicleId == null || vehicleId.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setString(1, vehicleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapVehicle(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to retrieve vehicle: " + vehicleId, exception);
        }
    }

    @Override
    public void save(Vehicle vehicle) {
        if (vehicle == null || vehicle.id() == null || vehicle.id().isBlank()) {
            throw new IllegalArgumentException("Vehicle and vehicle ID are required");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setString(1, vehicle.id());
            statement.setString(2, vehicle.name());
            statement.setString(3, vehicle.type());
            setNullableDouble(statement, 4, vehicle.lat());
            setNullableDouble(statement, 5, vehicle.lon());
            statement.setString(6, vehicle.status().name());
            statement.setInt(7, vehicle.batteryPct());
            statement.setString(8, vehicle.qrCode());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save vehicle: " + vehicle.id(), exception);
        }
    }

    private Vehicle mapVehicle(ResultSet resultSet) throws SQLException {
        return new Vehicle(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("type"),
                getNullableDouble(resultSet, "lat"),
                getNullableDouble(resultSet, "lon"),
                VehicleStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("battery_pct"),
                resultSet.getString("qr_code")
        );
    }

    private void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    private Double getNullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }
}
