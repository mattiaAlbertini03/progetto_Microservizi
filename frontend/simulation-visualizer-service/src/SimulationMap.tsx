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

const markerIcon = L.divIcon({
    className: "vehicle-marker-shell",
    html: '<div class="vehicle-marker">🚙</div>',
    iconSize: [52, 52],
    iconAnchor: [26, 26],
    popupAnchor: [0, -27],
});

function MapFollower({ position }: { position: LatLngExpression | null }) {
    const map = useMap();

    useEffect(() => {
        if (position) map.panTo(position, { animate: true, duration: 0.45 });
    }, [map, position]);

    return null;
}

export default function SimulationMap() {
    const queryParams = useMemo(() => new URLSearchParams(window.location.search), []);
    const reservationId = queryParams.get("reservationId");
    const vehicleId = queryParams.get("vehicleId");
    const [vehicle, setVehicle] = useState<VehicleResponse | null>(null);
    const [history, setHistory] = useState<LatLngExpression[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!vehicleId) {
            setVehicle(null);
            setHistory([]);
            setError("Nessun vehicleId specificato nell'indirizzo.");
            return;
        }

        const trackedVehicleId = vehicleId;
        let cancelled = false;

        async function fetchVehicle(): Promise<void> {
            try {
                const response = await fetch(`${API_BASE}/vehicles/${encodeURIComponent(trackedVehicleId)}`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);

                const data = (await response.json()) as VehicleResponse;
                if (cancelled) return;

                setError(null);
                setVehicle(data);
                setHistory((previous) => {
                    const nextPoint: LatLngExpression = [data.lat, data.lon];
                    const lastPoint = previous[previous.length - 1] as [number, number] | undefined;

                    if (lastPoint && lastPoint[0] === data.lat && lastPoint[1] === data.lon) {
                        return previous;
                    }
                    return [...previous.slice(-99), nextPoint];
                });
            } catch (exception) {
                console.error("Errore recupero posizione veicolo:", exception);
                if (!cancelled) setError("Impossibile recuperare la posizione del veicolo.");
            }
        }

        setVehicle(null);
        setHistory([]);
        setError(null);
        void fetchVehicle();
        const intervalId = window.setInterval(() => void fetchVehicle(), POLL_MS);

        return () => {
            cancelled = true;
            window.clearInterval(intervalId);
        };
    }, [vehicleId]);

    const center = useMemo<LatLngExpression>(
        () => vehicle ? [vehicle.lat, vehicle.lon] : [44.4949, 11.3426],
        [vehicle]
    );
    const battery = Math.max(0, Math.min(100, vehicle?.batteryPercent ?? 0));
    const updatedAt = vehicle
        ? new Date(vehicle.trackingTimestampEpochMs).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
        : "In attesa";
    const connectionState = error ? "error" : vehicle ? "live" : "waiting";

    return (
        <div className="visualizer-shell">
            <header className="visualizer-header">
                <div className="visualizer-brand">
                    <span className="visualizer-logo">A</span>
                    <span>ACMEMobility <small>Live fleet telemetry</small></span>
                </div>
                <span className={`connection-pill connection-pill--${connectionState}`}>
                    <i className="connection-dot" />
                    <span>{error ? "Connessione interrotta" : vehicle ? "Telemetria attiva" : "Connessione…"}</span>
                </span>
            </header>

            <main className="visualizer-main">
                <aside className="telemetry-panel">
                    <p className="telemetry-eyebrow">Corsa in tempo reale</p>
                    <h1>Segui il viaggio</h1>
                    <p className="telemetry-intro">Posizione e batteria aggiornate automaticamente durante il noleggio.</p>

                    <div className="trip-reference">
                        <div className="reference-row">
                            <span>Veicolo</span>
                            <strong>{vehicleId ?? "Non specificato"}</strong>
                        </div>
                        <div className="reference-row">
                            <span>Prenotazione</span>
                            <strong>{reservationId ? `#${reservationId.slice(0, 12)}` : "Non specificata"}</strong>
                        </div>
                    </div>

                    <section className="battery-card">
                        <div className="battery-card-header">
                            <span>Batteria residua</span>
                            <strong>{vehicle ? `${Math.round(battery)}%` : "—"}</strong>
                        </div>
                        <div className="battery-meter"><i style={{ width: `${battery}%` }} /></div>
                        <small>{battery < 15 && vehicle ? "Livello basso: alla riconsegna si applica la penale." : "Consumo monitorato in tempo reale"}</small>
                    </section>

                    <div className="telemetry-grid">
                        <div className="telemetry-stat"><span>Ultimo dato</span><strong>{updatedAt}</strong></div>
                        <div className="telemetry-stat"><span>Punti percorso</span><strong>{history.length}</strong></div>
                        <div className="telemetry-stat"><span>Latitudine</span><strong>{vehicle ? vehicle.lat.toFixed(5) : "—"}</strong></div>
                        <div className="telemetry-stat"><span>Longitudine</span><strong>{vehicle ? vehicle.lon.toFixed(5) : "—"}</strong></div>
                    </div>

                    <p className="polling-note"><i className="pulse-icon" /> Aggiornamento automatico ogni {POLL_MS / 1000} secondi</p>
                </aside>

                <section className="visualizer-map-card">
                    <div className="map-overlay">
                        <span className="map-overlay-icon">⌖</span>
                        <div><strong>Vista percorso</strong><small>{history.length > 1 ? "Traccia GPS attiva" : "Acquisizione percorso"}</small></div>
                    </div>

                    <MapContainer center={center} zoom={15} zoomControl={false} className="tracking-map">
                        <MapFollower position={vehicle ? [vehicle.lat, vehicle.lon] : null} />
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        {history.length > 1 && <Polyline positions={history} pathOptions={{ color: "#16754a", weight: 5, opacity: 0.85 }} />}
                        {history.map((point, index) => (
                            <CircleMarker key={`${index}-${String(point)}`} center={point} radius={2.5} pathOptions={{ color: "#4be087", fillOpacity: 0.9 }} />
                        ))}
                        {vehicle && (
                            <Marker position={[vehicle.lat, vehicle.lon]} icon={markerIcon}>
                                <Popup>
                                    <strong>{vehicle.vehicleId}</strong><br />
                                    Batteria {Math.round(vehicle.batteryPercent)}%<br />
                                    {vehicle.lat.toFixed(5)}, {vehicle.lon.toFixed(5)}
                                </Popup>
                            </Marker>
                        )}
                    </MapContainer>

                    {!vehicleId && (
                        <div className="empty-tracking"><strong>Manca il veicolo da tracciare</strong><p>Apri questa pagina dalla sezione Prenotazioni dell'app.</p></div>
                    )}
                    {error && vehicleId && (
                        <div className="visualizer-alert"><strong>Telemetria non disponibile</strong><p>{error} Il collegamento verrà ritentato automaticamente.</p></div>
                    )}
                </section>
            </main>
        </div>
    );
}
