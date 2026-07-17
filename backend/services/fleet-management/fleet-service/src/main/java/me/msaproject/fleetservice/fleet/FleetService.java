package me.msaproject.fleetservice.fleet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.fleetservice.client.LogisticsClient;
import me.msaproject.fleetservice.client.ReservationClient;
import me.msaproject.fleetservice.client.SimulatorClient;
import me.msaproject.fleetservice.client.StationClient;
import me.msaproject.fleetservice.client.VehicleQueryClient;
import me.msaproject.fleetservice.dto.FleetStatusResponse;
import me.msaproject.fleetservice.dto.FleetStopResponse;
import me.msaproject.fleetservice.dto.Reservation;
import me.msaproject.fleetservice.dto.SimulationStatusResponse;
import me.msaproject.fleetservice.dto.SimulationSummary;
import me.msaproject.fleetservice.dto.StartSimulationRequest;
import me.msaproject.fleetservice.dto.Station;
import me.msaproject.fleetservice.dto.StopSimulationRequest;
import me.msaproject.fleetservice.dto.Vehicle;
import me.msaproject.fleetservice.dto.VehicleAtStationResponse;
import me.msaproject.fleetservice.dto.VehicleSnapshot;
import me.msaproject.fleetservice.dto.VehicleTrackingResponse;
import me.msaproject.fleetservice.utils.GeoUtils;

@ApplicationScoped
public class FleetService {

    private static final double SIMULATION_SPEED_METERS_PER_SECOND = 8.0;
    private static final double STATION_PROXIMITY_METERS = 30.0;

    /**
     * The simulator assigns one deterministic destination per active reservation.
     * Keeping that association here makes all later status and return checks use
     * the same station selected when the trip started.
     */
    private final Map<String, String> destinationByReservationId = new ConcurrentHashMap<>();

    @Inject
    ReservationClient reservationClient;

    @Inject
    LogisticsClient logisticsClient;

    @Inject
    StationClient stationClient;

    @Inject
    SimulatorClient simulatorClient;

    @Inject
    VehicleQueryClient vehicleQueryClient;

    public void startSimulation(String reservationId) {
        Reservation reservation = reservationClient.getReservation(reservationId);
        Vehicle vehicle = logisticsClient.getVehicle(reservation.vehicleId());
        List<Station> stations = stationClient.getStations();

        Station origin = findStation(stations, reservation.stationId());
        Station destination = destinationForReservation(stations, origin.id(), reservationId);
        destinationByReservationId.put(reservationId, destination.id());

        simulatorClient.startSimulation(new StartSimulationRequest(
                reservation.vehicleId(),
                origin.id(),
                destination.id(),
                origin.lat(),
                origin.lon(),
                destination.lat(),
                destination.lon(),
                SIMULATION_SPEED_METERS_PER_SECOND,
                vehicle.batteryPct()));
    }

    public FleetStopResponse stopSimulation(String reservationId) {
        Reservation reservation = reservationClient.getReservation(reservationId);
        SimulationSummary summary = simulatorClient.stopSimulation(
                new StopSimulationRequest(reservation.vehicleId()));

        destinationByReservationId.remove(reservationId);

        return new FleetStopResponse(
                reservationId,
                reservation.vehicleId(),
                summary.minutesUsed(),
                summary.kilometers(),
                summary.batteryPct(),
                summary.minimumBatteryPct());
    }

    public FleetStatusResponse getStatus(String reservationId) {
        Reservation reservation = reservationClient.getReservation(reservationId);
        VehicleSnapshot snapshot = vehicleQueryClient.getVehicle(reservation.vehicleId());
        SimulationStatusResponse simulationStatus = simulatorClient.getStatus(reservation.vehicleId());

        return new FleetStatusResponse(
                reservationId,
                snapshot.vehicleId(),
                snapshot.lat(),
                snapshot.lon(),
                snapshot.batteryPercent(),
                simulationStatus.movementStatus(),
                simulationStatus.kilometers(),
                simulationStatus.minutesUsed(),
                snapshot.trackingTimestampEpochMs(),
                snapshot.batteryTimestampEpochMs());
    }

    public VehicleAtStationResponse isVehicleAtStation(String reservationId) {
        Reservation reservation = reservationClient.getReservation(reservationId);
        VehicleTrackingResponse tracking = vehicleQueryClient.getVehicleTracking(reservation.vehicleId());
        SimulationStatusResponse simulationStatus = simulatorClient.getStatus(reservation.vehicleId());

        String destinationStationId = destinationByReservationId.get(reservationId);
        if (destinationStationId == null) {
            throw new IllegalStateException("Destination not available for reservation: " + reservationId);
        }

        Station destination = findStation(stationClient.getStations(), destinationStationId);
        boolean atStation = GeoUtils.distanceMeters(
                tracking.lat(), tracking.lon(),
                destination.lat(), destination.lon()) <= STATION_PROXIMITY_METERS;
        boolean stopped = "STOPPED".equals(simulationStatus.movementStatus());

        return new VehicleAtStationResponse(
                atStation,
                stopped,
                simulationStatus.movementStatus(),
                destination.id());
    }

    public void rechargeAndResume(String reservationId) {
        Reservation reservation = reservationClient.getReservation(reservationId);
        simulatorClient.rechargeAndResume(reservation.vehicleId());
    }

    private Station findStation(List<Station> stations, String stationId) {
        return stations.stream()
                .filter(station -> station.id().equals(stationId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Station not found: " + stationId));
    }

    private Station destinationForReservation(
            List<Station> stations,
            String originStationId,
            String reservationId) {

        List<Station> candidates = stations.stream()
                .filter(station -> !station.id().equals(originStationId))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No destination stations available");
        }

        int destinationIndex = Math.floorMod(reservationId.hashCode(), candidates.size());
        return candidates.get(destinationIndex);
    }
}
