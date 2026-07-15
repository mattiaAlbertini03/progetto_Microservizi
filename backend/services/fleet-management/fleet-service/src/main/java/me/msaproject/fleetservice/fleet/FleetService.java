package me.msaproject.fleetservice.fleet;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.msaproject.fleetservice.client.LogisticsClient;
import me.msaproject.fleetservice.client.ReservationClient;
import me.msaproject.fleetservice.client.SimulatorClient;
import me.msaproject.fleetservice.client.StationClient;
import me.msaproject.fleetservice.client.VehicleQueryClient;
import me.msaproject.fleetservice.dto.*;
import me.msaproject.fleetservice.utils.GeoUtils;

@ApplicationScoped
public class FleetService {

        private final Random random = new Random();

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
                System.out.println("[Fleet] Loading reservation");
                Reservation reservation = reservationClient.getReservation(reservationId);

                System.out.println("[Fleet] Loading vehicle");
                Vehicle vehicle = logisticsClient.getVehicle(reservation.vehicleId());

                System.out.println("[Fleet] Loading stations");
                List<Station> stations = stationClient.getStations();

                System.out.println("[Fleet] Selecting destination");
                Station fromStation = findStation(stations, reservation.stationId());

                Station toStation = pickRandomDestination(stations, fromStation.id());

                destinationByReservationId.put(
                                reservationId,
                                toStation.id());

                StartSimulationRequest request = new StartSimulationRequest(
                                reservation.vehicleId(),
                                fromStation.id(),
                                toStation.id(),
                                fromStation.lat(),
                                fromStation.lon(),
                                toStation.lat(),
                                toStation.lon(),
                                8.0,
                                vehicle.batteryPct());

                System.out.println("[Fleet] Calling simulator");
                simulatorClient.startSimulation(request);

                System.out.println("[Fleet] Simulation started");
        }

        public void stopSimulation(String reservationId) {
                Reservation reservation = reservationClient.getReservation(reservationId);

                simulatorClient.stopSimulation(
                                new StopSimulationRequest(reservation.vehicleId()));

                destinationByReservationId.remove(reservationId);
        }

        public FleetStatusResponse getStatus(String reservationId) {

                Reservation reservation = reservationClient.getReservation(reservationId);

                VehicleSnapshot snapshot = vehicleQueryClient.getVehicle(reservation.vehicleId());

                return new FleetStatusResponse(
                                reservationId,
                                snapshot.vehicleId(),
                                snapshot.lat(),
                                snapshot.lon(),
                                snapshot.batteryPercent(),
                                snapshot.trackingTimestampEpochMs(),
                                snapshot.batteryTimestampEpochMs());
        }

        public boolean isVehicleAtDestination(String reservationId) {
                Reservation reservation = reservationClient.getReservation(reservationId);

                String destinationStationId = destinationByReservationId.get(reservationId);

                if (destinationStationId == null) {
                        throw new IllegalStateException(
                                        "No destination station found for reservation " + reservationId);
                }

                VehicleTrackingResponse tracking = vehicleQueryClient.getVehicleTracking(reservation.vehicleId());

                Station destinationStation = findStation(stationClient.getStations(), destinationStationId);

                double distanceMeters = GeoUtils.distanceMeters(
                                tracking.lat(),
                                tracking.lon(),
                                destinationStation.lat(),
                                destinationStation.lon());

                return distanceMeters <= 30.0;
        }

        private Station findStation(List<Station> stations, String stationId) {
                return stations.stream()
                                .filter(station -> station.id().equals(stationId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "Station not found: " + stationId));
        }

        private Station pickRandomDestination(List<Station> stations, String fromStationId) {
                List<Station> candidates = stations.stream()
                                .filter(station -> !station.id().equals(fromStationId))
                                .toList();

                if (candidates.isEmpty()) {
                        throw new IllegalStateException("No destination stations available");
                }

                return candidates.get(random.nextInt(candidates.size()));
        }
}