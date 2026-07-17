package me.msaproject.authservice.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.authservice.auth.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class PostgresUsersRepository implements UsersRepository {

    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, username, password, email
            FROM users
            WHERE username = ?
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id, username, password, email
            FROM users
            WHERE id = ?
            """;

    private static final String SAVE_SQL = """
            INSERT INTO users (id, username, password, email)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (id)
            DO UPDATE SET
                username = EXCLUDED.username,
                password = EXCLUDED.password,
                email = EXCLUDED.email
            """;

    private static final String EXISTS_BY_USERNAME_SQL = """
            SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
            """;

    private static final String EXISTS_BY_EMAIL_SQL = """
            SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private HikariDataSource dataSource;

    @Inject
    public PostgresUsersRepository(
            @ConfigProperty(name = "auth-db.url") String jdbcUrl,
            @ConfigProperty(name = "auth-db.user") String username,
            @ConfigProperty(name = "auth-db.password") String password,
            @ConfigProperty(name = "auth-db.pool.maximum-size", defaultValue = "4")
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
        config.setPoolName("auth-db-pool");
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
    public Optional<User> findByUsername(String username) {
        return findOne(FIND_BY_USERNAME_SQL, username);
    }

    @Override
    public Optional<User> findById(String id) {
        return findOne(FIND_BY_ID_SQL, id);
    }

    @Override
    public User save(User user) {
        if (user == null || user.id() == null || user.id().isBlank()) {
            throw new IllegalArgumentException("User and user ID are required");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL)) {
            statement.setString(1, user.id());
            statement.setString(2, user.username());
            statement.setString(3, user.password());
            statement.setString(4, user.email());
            statement.executeUpdate();
            return user;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save user: " + user.id(), exception);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return exists(EXISTS_BY_USERNAME_SQL, username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists(EXISTS_BY_EMAIL_SQL, email);
    }

    private Optional<User> findOne(String sql, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(mapUser(resultSet))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to retrieve user", exception);
        }
    }

    private boolean exists(String sql, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to check user existence", exception);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getString("id"),
                resultSet.getString("username"),
                resultSet.getString("password"),
                resultSet.getString("email")
        );
    }
}
