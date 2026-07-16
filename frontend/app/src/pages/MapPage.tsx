import { useCallback, useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, TileLayer, useMap } from "react-leaflet";
import type { LatLngExpression } from "leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import Station from "../components/Station";
import { useAuth } from "../components/AuthContext";
import { useReservationEvents } from "../components/ReservationEventsContext";
import { useVehicleEvents } from "../hooks/useVehicleEvents";
import {
    createReservation,
    createQrReservation,
    getStations,
    getVehiclesByStation,
} from "../api/acmeApi";
import type { StationData, VehicleData } from "../types/domain";

const stationIcon = L.divIcon({
    className: "station-marker-shell",
    html: '<div class="station-marker"><span>⚡</span></div>',
    iconSize: [48, 54],
    iconAnchor: [24, 50],
});

function MapFocus({ position }: { position: LatLngExpression | null }) {
    const map = useMap();

    useEffect(() => {
        if (position) {
            map.flyTo(position, Math.max(map.getZoom(), 15), { duration: 0.55 });
        }
    }, [map, position]);

    return null;
}

export default function MapPage() {
    const { user, openLogin } = useAuth();
    const [stations, setStations] = useState<StationData[]>([]);
    const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
    const [vehiclesByStation, setVehiclesByStation] = useState<Record<string, VehicleData[]>>({});
    const [loadingStationId, setLoadingStationId] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const { listenReservationRequest } = useReservationEvents();

    const selectedStation = useMemo(
        () => stations.find((station) => station.id === selectedStationId) ?? null,
        [selectedStationId, stations]
    );

    useEffect(() => {
        async function loadStations() {
            try {
                const data = await getStations();
                setStations(data);
                setError(null);
            } catch (err) {
                console.error(err);
                setError("Impossibile caricare le stazioni. Riprova tra poco.");
            }
        }

        void loadStations();
    }, []);

    const refreshVehicles = useCallback(async (stationId: string) => {
        try {
            setLoadingStationId(stationId);
            const vehicles = await getVehiclesByStation(stationId);
            setVehiclesByStation((previous) => ({ ...previous, [stationId]: vehicles }));
            setError(null);
        } catch (err) {
            console.error(err);
            setError("Impossibile caricare i veicoli della stazione.");
        } finally {
            setLoadingStationId(null);
        }
    }, []);

    useEffect(() => {
        async function onReservationConfirmed(event: Event) {
            const customEvent = event as CustomEvent<{ stationId?: string }>;
            if (customEvent.detail.stationId) {
                await refreshVehicles(customEvent.detail.stationId);
            }
        }

        window.addEventListener("reservation-confirmed", onReservationConfirmed);
        return () => window.removeEventListener("reservation-confirmed", onReservationConfirmed);
    }, [refreshVehicles]);

    const selectStation = useCallback(async (stationId: string) => {
        setSelectedStationId(stationId);
        if (!vehiclesByStation[stationId]) {
            await refreshVehicles(stationId);
        }
    }, [refreshVehicles, vehiclesByStation]);

    const refreshSelectedStationVehicles = useCallback(() => {
        if (selectedStationId) void refreshVehicles(selectedStationId);
    }, [selectedStationId, refreshVehicles]);

    useVehicleEvents(selectedStationId, refreshSelectedStationVehicles);

    async function reserveVehicle(
        station: StationData,
        vehicle: VehicleData,
        scheduledStartEpochMs: number
    ) {
        if (!user) {
            openLogin();
            return;
        }

        try {
            const data = await createReservation(
                user,
                vehicle.id,
                station.id,
                scheduledStartEpochMs
            );
            listenReservationRequest(data.requestId, station.id);
            alert("Richiesta di prenotazione inviata");
        } catch (err) {
            console.error("Errore durante la prenotazione:", err);
            alert(err instanceof Error ? err.message : "Errore durante la prenotazione");
        }
    }

    async function pickupWithQr(station: StationData, vehicle: VehicleData) {
        if (!user) {
            openLogin();
            return;
        }

        const scannedCode = window.prompt("Scansiona o inserisci il codice QR del veicolo");
        if (!scannedCode) return;

        try {
            const data = await createQrReservation(
                user,
                vehicle.id,
                station.id,
                scannedCode
            );
            listenReservationRequest(data.requestId, station.id);
            alert("Richiesta di ritiro tramite QR inviata");
        } catch (err) {
            console.error("Errore durante il ritiro tramite QR:", err);
            alert(err instanceof Error ? err.message : "Errore durante il ritiro QR");
        }
    }

    return (
        <main className="map-page">
            <div className="map-heading">
                <div>
                    <span className="map-live-dot" />
                    <span>{stations.length} stazioni connesse</span>
                </div>
                <strong>Trova il tuo prossimo mezzo</strong>
            </div>

            <div className="station-strip" aria-label="Stazioni disponibili">
                {stations.map((station) => (
                    <button
                        key={station.id}
                        className={`station-chip${selectedStationId === station.id ? " active" : ""}`}
                        onClick={() => void selectStation(station.id)}
                    >
                        <span className="station-chip-icon">⚡</span>
                        <span>
                            <strong>{station.name}</strong>
                            <small>{station.capacity} posti</small>
                        </span>
                    </button>
                ))}
            </div>

            {error && <div className="map-alert alert">{error}</div>}

            <MapContainer
                center={[44.4949, 11.3426]}
                zoom={14}
                zoomControl={false}
                className="mobility-map"
            >
                <MapFocus
                    position={selectedStation ? [selectedStation.lat, selectedStation.lon] : null}
                />
                <TileLayer
                    attribution="&copy; OpenStreetMap contributors"
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {stations.map((station) => (
                    <Marker
                        key={station.id}
                        position={[station.lat, station.lon]}
                        icon={stationIcon}
                        eventHandlers={{ click: () => void selectStation(station.id) }}
                    />
                ))}
            </MapContainer>

            {selectedStation ? (
                <aside className="station-sheet" aria-label={`Dettagli ${selectedStation.name}`}>
                    <button
                        className="sheet-close icon-button"
                        onClick={() => setSelectedStationId(null)}
                        aria-label="Chiudi dettagli stazione"
                    >
                        ×
                    </button>
                    <Station
                        station={selectedStation}
                        vehicles={vehiclesByStation[selectedStation.id]}
                        loading={loadingStationId === selectedStation.id}
                        onReserve={reserveVehicle}
                        onQrPickup={pickupWithQr}
                    />
                </aside>
            ) : (
                <div className="map-hint">
                    <span>⚡</span>
                    <div>
                        <strong>Scegli una stazione</strong>
                        <small>Tocca un indicatore per vedere i mezzi disponibili</small>
                    </div>
                </div>
            )}
        </main>
    );
}
