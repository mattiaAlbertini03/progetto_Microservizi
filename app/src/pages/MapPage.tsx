import { useCallback, useEffect, useState } from "react";
import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import Station from "../components/Station";
import { useAuth } from "../components/AuthContext";
import { ACME_API_URL } from "../App";
import { useVehicleEvents } from "../hooks/useVehicleEvents";
import {
    createReservation,
    getStations,
    getVehiclesByStation,
} from "../api/acmeApi";
import type { StationData, VehicleData } from "../types/domain";

const defaultIcon = L.icon({
    iconRetinaUrl:
        "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
    iconUrl:
        "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    shadowUrl:
        "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
});

export default function MapPage() {
    const { user } = useAuth();

    const [stations, setStations] = useState<StationData[]>([]);
    const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
    const [vehiclesByStation, setVehiclesByStation] = useState<
        Record<string, VehicleData[]>
    >({});
    const [loadingStationId, setLoadingStationId] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadStations();
    }, []);

    async function loadStations() {
        try {
            const data = await getStations();
            setStations(data);
        } catch (err) {
            console.error(err);
            setError("Impossibile caricare le stazioni");
        }
    }

    const refreshVehicles = useCallback(async (stationId: string) => {
        try {
            setLoadingStationId(stationId);

            const vehicles = await getVehiclesByStation(stationId);

            setVehiclesByStation((prev) => ({
                ...prev,
                [stationId]: vehicles,
            }));
        } catch (err) {
            console.error(err);
            setError("Impossibile caricare i veicoli della stazione");
        } finally {
            setLoadingStationId(null);
        }
    }, []);

    const loadVehicles = useCallback(
        async (stationId: string) => {
            setSelectedStationId(stationId);

            if (vehiclesByStation[stationId]) {
                return;
            }

            await refreshVehicles(stationId);
        },
        [vehiclesByStation, refreshVehicles]
    );

    const refreshSelectedStationVehicles = useCallback(() => {
        if (selectedStationId) {
            refreshVehicles(selectedStationId);
        }
    }, [selectedStationId, refreshVehicles]);

    useVehicleEvents(selectedStationId, refreshSelectedStationVehicles);

    async function reserveVehicle(station: StationData, vehicle: VehicleData) {
        if (!user) {
            return;
        }

        try {
            const data = await createReservation(user.id, vehicle.id, station.id);
            const requestId = data.requestId;

            const source = new EventSource(
                `${ACME_API_URL}/reservations/${requestId}/events`
            );

            source.addEventListener("reservation-update", async (event) => {
                const update = JSON.parse((event as MessageEvent).data);

                if (update.status === "RESERVED") {
                    source.close();
                    await refreshVehicles(station.id);
                    alert("Prenotazione confermata");
                }

                if (update.status === "FAILED") {
                    source.close();
                    alert(update.message ?? "Prenotazione fallita");
                }
            });

            source.onerror = (err) => {
                console.error("Reservation SSE error:", err);
            };
        } catch (err) {
            console.error(err);
            alert("Errore durante la prenotazione");
        }
    }

    return (
        <div style={{ height: "100vh", width: "100%" }}>
            {error && (
                <div
                    style={{
                        position: "absolute",
                        top: "72px",
                        left: "1rem",
                        zIndex: 1000,
                        padding: "0.75rem 1rem",
                        background: "white",
                        color: "red",
                        borderRadius: "8px",
                        boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                    }}
                >
                    {error}
                </div>
            )}

            <MapContainer
                center={[44.4949, 11.3426]}
                zoom={14}
                style={{ height: "100%", width: "100%" }}
            >
                <TileLayer
                    attribution="&copy; OpenStreetMap contributors"
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {stations.map((station) => (
                    <Marker
                        key={station.id}
                        position={[station.lat, station.lon]}
                        icon={defaultIcon}
                        eventHandlers={{
                            click: () => loadVehicles(station.id),
                        }}
                    >
                        <Popup minWidth={300}>
                            <Station
                                station={station}
                                vehicles={vehiclesByStation[station.id]}
                                loading={loadingStationId === station.id}
                                onReserve={user ? reserveVehicle : undefined}
                            />
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}