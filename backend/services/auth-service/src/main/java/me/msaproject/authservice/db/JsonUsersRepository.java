package me.msaproject.authservice.db;

import me.msaproject.authservice.auth.User;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JsonUsersRepository implements UsersRepository {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();

    public JsonUsersRepository() {
        User demo = new User(
                "user-1",
                "demo",
                "demo",
                "demo@acme.local"
        );

        usersById.put(demo.id(), demo);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return usersById.values()
                .stream()
                .filter(user -> user.username().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public User save(User user) {
        usersById.put(user.id(), user);
        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersById.values()
                .stream()
                .anyMatch(user -> user.email().equals(email));
    }
}
