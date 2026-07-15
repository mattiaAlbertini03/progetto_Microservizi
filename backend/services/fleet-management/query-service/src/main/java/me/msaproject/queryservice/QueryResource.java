package me.msaproject.queryservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource {

    @Inject
    VehicleStore store;

    @GET
    public Collection<VehicleSnapshot> all() {
        return store.all();
    }

    @GET
    @Path("/{id}")
    public Response get(
            @PathParam("id") String vehicleId
    ) {
        VehicleSnapshot snapshot =
                store.get(vehicleId);

        if (snapshot == null) {
            return vehicleNotFound(vehicleId);
        }

        return Response.ok(snapshot).build();
    }

    @GET
    @Path("/{id}/tracking")
    public Response tracking(
            @PathParam("id") String vehicleId
    ) {
        VehicleSnapshot snapshot =
                store.get(vehicleId);

        if (snapshot == null) {
            return vehicleNotFound(vehicleId);
        }

        if (!snapshot.hasTracking()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error",
                            "tracking_not_available",
                            "vehicleId",
                            vehicleId
                    ))
                    .build();
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "vehicleId",
                snapshot.vehicleId()
        );

        response.put(
                "lat",
                snapshot.lat()
        );

        response.put(
                "lon",
                snapshot.lon()
        );

        response.put(
                "timestampEpochMs",
                snapshot.trackingTimestampEpochMs()
        );

        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}/battery")
    public Response battery(
            @PathParam("id") String vehicleId
    ) {
        VehicleSnapshot snapshot =
                store.get(vehicleId);

        if (snapshot == null) {
            return vehicleNotFound(vehicleId);
        }

        if (!snapshot.hasBattery()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error",
                            "battery_not_available",
                            "vehicleId",
                            vehicleId
                    ))
                    .build();
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "vehicleId",
                snapshot.vehicleId()
        );

        response.put(
                "batteryPercent",
                snapshot.batteryPercent()
        );

        response.put(
                "timestampEpochMs",
                snapshot.batteryTimestampEpochMs()
        );

        return Response.ok(response).build();
    }

    private Response vehicleNotFound(
            String vehicleId
    ) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                        "error",
                        "vehicle_not_found",
                        "vehicleId",
                        vehicleId
                ))
                .build();
    }
}