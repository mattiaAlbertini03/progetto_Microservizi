import { useEffect, useMemo, useState } from "react";
import {
    CircleMarker,
    MapContainer,
    Marker,
    Polyline,
    Popup,
    TileLayer,
    useMap,
} from "react-leaflet";
import type { LatLngExpression } from "leaflet";
import * as L from "leaflet";
import "leaflet/dist/leaflet.css";

const API_BASE = "/api";
const POLL_MS = 2000;

type VehicleResponse = {
    vehicleId: string;
    lat: number;
    lon: number;
    trackingTimestampEpochMs: number;
    batteryPercent: number;
    batteryTimestampEpochMs: number;
};

const markerIcon = new L.Icon({
    iconUrl:
        "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    shadowUrl:
        "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
});

function MapFollower({
    position,
}: {
    position: LatLngExpression | null;
}) {
    const map = useMap();

    useEffect(() => {
        if (position) {
            map.setView(position);
        }
    }, [map, position]);

    return null;
}

export default function SimulationMap() {
    const queryParams = useMemo(
        () => new URLSearchParams(window.location.search),
        []
    );

    const reservationId = queryParams.get("reservationId");
    const vehicleId = queryParams.get("vehicleId");

    const [vehicle, setVehicle] =
        useState<VehicleResponse | null>(null);

    const [history, setHistory] =
        useState<LatLngExpression[]>([]);

    const [error, setError] =
        useState<string | null>(null);

    useEffect(() => {
        if (!vehicleId) {
            setVehicle(null);
            setHistory([]);
            setError(
                "Nessun vehicleId specificato nell'indirizzo."
            );
            return;
        }

        /*
         * Dopo questo controllo sappiamo che vehicleId è una stringa.
         * La copiamo in una costante per mantenere il tipo string anche
         * dentro la funzione asincrona e il callback dell'intervallo.
         */
        const trackedVehicleId = vehicleId;
        let cancelled = false;

        async function fetchVehicle(): Promise<void> {
            try {
                const response = await fetch(
                    `${API_BASE}/vehicles/${encodeURIComponent(
                        trackedVehicleId
                    )}`
                );

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }

                const data =
                    (await response.json()) as VehicleResponse;

                if (cancelled) {
                    return;
                }

                setError(null);
                setVehicle(data);

                setHistory((previous) => {
                    const nextPoint: LatLngExpression = [
                        data.lat,
                        data.lon,
                    ];

                    const lastPoint = previous[
                        previous.length - 1
                    ] as [number, number] | undefined;

                    if (
                        lastPoint &&
                        lastPoint[0] === data.lat &&
                        lastPoint[1] === data.lon
                    ) {
                        return previous;
                    }

                    return [
                        ...previous.slice(-99),
                        nextPoint,
                    ];
                });
            } catch (exception) {
                console.error(
                    "Errore recupero posizione veicolo:",
                    exception
                );

                if (!cancelled) {
                    setError(
                        "Impossibile recuperare la posizione del veicolo."
                    );
                }
            }
        }

        setVehicle(null);
        setHistory([]);
        setError(null);

        void fetchVehicle();

        const intervalId = window.setInterval(
            () => {
                void fetchVehicle();
            },
            POLL_MS
        );

        return () => {
            cancelled = true;
            window.clearInterval(intervalId);
        };
    }, [vehicleId]);

    const center = useMemo<LatLngExpression>(() => {
        if (vehicle) {
            return [vehicle.lat, vehicle.lon];
        }

        return [44.4949, 11.3426];
    }, [vehicle]);

    return (
        <div
            style={{
                minHeight: "100vh",
                padding: "24px",
                background: "#f8fafc",
            }}
        >
            <h1>Tracciamento prenotazione</h1>

            {reservationId && (
                <p>
                    Prenotazione:{" "}
                    <strong>{reservationId}</strong>
                </p>
            )}

            {vehicleId && (
                <p>
                    Veicolo: <strong>{vehicleId}</strong>
                </p>
            )}

            <p>
                Aggiornamento posizione ogni{" "}
                {POLL_MS / 1000} secondi.
            </p>

            {vehicle && (
                <div style={{ marginBottom: "16px" }}>
                    <strong>{vehicle.vehicleId}</strong>
                    {" — "}
                    batteria {vehicle.batteryPercent}%
                    {" — "}
                    lat {vehicle.lat.toFixed(6)}
                    {" — "}
                    lon {vehicle.lon.toFixed(6)}
                </div>
            )}

            {error && (
                <div
                    style={{
                        color: "red",
                        marginBottom: "16px",
                    }}
                >
                    Errore: {error}
                </div>
            )}

            <MapContainer
                center={center}
                zoom={14}
                style={{
                    height: "70vh",
                    width: "100%",
                }}
            >
                <MapFollower
                    position={
                        vehicle
                            ? [vehicle.lat, vehicle.lon]
                            : null
                    }
                />

                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {history.length > 1 && (
                    <Polyline positions={history} />
                )}

                {history.map((point, index) => (
                    <CircleMarker
                        key={`${index}-${String(point)}`}
                        center={point}
                        radius={3}
                    />
                ))}

                {vehicle && (
                    <Marker
                        position={[
                            vehicle.lat,
                            vehicle.lon,
                        ]}
                        icon={markerIcon}
                    >
                        <Popup>
                            <div>
                                <div>
                                    <strong>Veicolo:</strong>{" "}
                                    {vehicle.vehicleId}
                                </div>

                                <div>
                                    <strong>Batteria:</strong>{" "}
                                    {vehicle.batteryPercent}%
                                </div>

                                <div>
                                    <strong>Latitudine:</strong>{" "}
                                    {vehicle.lat.toFixed(6)}
                                </div>

                                <div>
                                    <strong>Longitudine:</strong>{" "}
                                    {vehicle.lon.toFixed(6)}
                                </div>
                            </div>
                        </Popup>
                    </Marker>
                )}
            </MapContainer>
        </div>
    );
}