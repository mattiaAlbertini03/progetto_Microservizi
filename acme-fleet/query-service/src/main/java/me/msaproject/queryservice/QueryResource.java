package me.msaproject.queryservice;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
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
    public Response get(@PathParam("id") String id) {
        var s = store.get(id);
        if (s == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "vehicle_not_found", "vehicleId", id))
                    .build();
        }
        return Response.ok(s).build();
    }

    @GET
    @Path("/{id}/tracking")
    public Response tracking(@PathParam("id") String id) {
        var s = store.get(id);
        if (s == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "vehicle_not_found", "vehicleId", id))
                    .build();
        }
        return Response.ok(Map.of(
                "vehicleId", s.vehicleId(),
                "lat", s.lat(),
                "lon", s.lon(),
                "timestampEpochMs", s.trackingTimestampEpochMs()
        )).build();
    }

    @GET
    @Path("/{id}/battery")
    public Response battery(@PathParam("id") String id) {
        var s = store.get(id);
        if (s == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "vehicle_not_found", "vehicleId", id))
                    .build();
        }
        return Response.ok(Map.of(
                "vehicleId", s.vehicleId(),
                "batteryPercent", s.batteryPercent(),
                "timestampEpochMs", s.batteryTimestampEpochMs()
        )).build();
    }
}
