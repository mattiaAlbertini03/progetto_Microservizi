package me.msaproject.acmemobilityservice.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PendingBankConnectionService {

    private final Map<String, String> ownersByRequestId = new ConcurrentHashMap<>();

    public void register(String requestId, String userId) {
        ownersByRequestId.put(requestId, userId);
    }

    public void remove(String requestId) {
        ownersByRequestId.remove(requestId);
    }

    public void requireNotOwnedByAnotherUser(String requestId, String userId) {
        String owner = ownersByRequestId.get(requestId);

        // This registry is an in-memory optimization and is intentionally
        // allowed to be empty after a gateway restart. A known different owner
        // is still rejected; an unknown UUID can be correlated by Camunda.
        if (owner != null && !userId.equals(owner)) {
            throw new WebApplicationException(
                    "The pending bank connection belongs to another user",
                    Response.Status.CONFLICT);
        }
    }

    public void consume(String requestId, String userId) {
        ownersByRequestId.remove(requestId, userId);
    }
}
