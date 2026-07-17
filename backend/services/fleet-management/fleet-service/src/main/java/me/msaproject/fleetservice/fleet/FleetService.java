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
import me.msaproject.fleetservice.dto.*;
import me.msaproject.fleetservice.utils.GeoUtils;

@ApplicationScoped
public class FleetService {

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

                Station toStation = destinationForReservation(stations, fromStation.id(), reservationId);

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
                        throw new IllegalStateException(
                                        "Destination not available for reservation: " + reservationId);
                }

                Station destination = findStation(stationClient.getStations(), destinationStationId);
                boolean atStation = GeoUtils.distanceMeters(
                                tracking.lat(), tracking.lon(),
                                destination.lat(), destination.lon()) <= 30.0;
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
                                .orElseThrow(() -> new IllegalStateException(
                                                "Station not found: " + stationId));
        }

        private Station destinationForReservation(List<Station> stations, String fromStationId, String reservationId) {
                List<Station> candidates = stations.stream()
                                .filter(station -> !station.id().equals(fromStationId))
                                .toList();
                if (candidates.isEmpty()) throw new IllegalStateException("No destination stations available");
                return candidates.get(Math.floorMod(reservationId.hashCode(), candidates.size()));
        }
}
