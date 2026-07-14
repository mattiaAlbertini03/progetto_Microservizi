package me.msaproject.authservice.db;

import java.util.Optional;

import me.msaproject.authservice.auth.User;

public interface UsersRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    User save(User user);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}