import { useEffect, useMemo, useState } from "react";
import { MapContainer, Marker, Popup, TileLayer, Polyline, CircleMarker } from "react-leaflet";
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
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

export default function SimulationMap() {
  const [vehicle, setVehicle] = useState<VehicleResponse | null>(null);
  const [history, setHistory] = useState<LatLngExpression[]>([]);
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<string>("");
  const [error] = useState<string | null>(null);

  const fetchVehicles = async () => {
      const response = await fetch(`${API_BASE}/vehicles`);
      const data = await response.json();
      setVehicles(data);

      if (data.length > 0 && selectedVehicleId === null) {
        setSelectedVehicleId(data[0].vehicleId);
      }
    };

  useEffect(() => {
    fetchVehicles();
  }, []);

  useEffect(() => {
    if (!selectedVehicleId) return;

    let cancelled = false;

    const fetchVehicle = async () => {
      try {
        const response = await fetch(`${API_BASE}/vehicles/${selectedVehicleId}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const data = await response.json();
        if (cancelled) return;

        setVehicle(data);

        setHistory((prev) => {
          const nextPoint: LatLngExpression = [data.lat, data.lon];
          const last = prev[prev.length - 1] as [number, number] | undefined;

          if (
            last &&
            Array.isArray(nextPoint) &&
            last[0] === nextPoint[0] &&
            last[1] === nextPoint[1]
          ) {
            return prev;
          }

          return [...prev.slice(-99), nextPoint];
        });
      } catch (e) {
        console.error(e);
      }
    };

    setHistory([]);
    fetchVehicle();
    const interval = setInterval(fetchVehicle, 2000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [selectedVehicleId]);

  const center = useMemo<LatLngExpression>(() => {
    if (vehicle) return [vehicle.lat, vehicle.lon];
    return [44.4949, 11.3426];
  }, [vehicle]);

  return (
    <div style={{ minHeight: "100vh", padding: "24px", background: "#f8fafc" }}>
      <h1>Vehicle Simulation Map</h1>
      <p>
        Polling ogni {POLL_MS / 1000}s su <select
          value={selectedVehicleId}
          onChange={(e) => setSelectedVehicleId(e.target.value)}
          onMouseOver={() => fetchVehicles()}
        >
          {vehicles.map((v) => (
            <option key={v.vehicleId} value={v.vehicleId}>
              {v.vehicleId}
            </option>
          ))}
        </select>
      </p>

      {vehicle && (
        <div style={{ marginBottom: "16px" }}>
          <strong>{vehicle.vehicleId}</strong> — batteria {vehicle.batteryPercent}% — lat{" "}
          {vehicle.lat.toFixed(6)} — lon {vehicle.lon.toFixed(6)}
        </div>
      )}

      {error && <div style={{ color: "red", marginBottom: "16px" }}>Errore: {error}</div>}

      <MapContainer center={center} zoom={14} style={{ height: "70vh", width: "100%" }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {history.length > 1 && <Polyline positions={history} />}

        {history.map((point, index) => (
          <CircleMarker key={index} center={point} radius={3} />
        ))}

        {vehicle && (
          <Marker position={[vehicle.lat, vehicle.lon]} icon={markerIcon}>
            <Popup>
              <div>
                <div>
                  <strong>Vehicle:</strong> {vehicle.vehicleId}
                </div>
                <div>
                  <strong>Battery:</strong> {vehicle.batteryPercent}%
                </div>
                <div>
                  <strong>Lat:</strong> {vehicle.lat.toFixed(6)}
                </div>
                <div>
                  <strong>Lon:</strong> {vehicle.lon.toFixed(6)}
                </div>
              </div>
            </Popup>
          </Marker>
        )}
      </MapContainer>
    </div>
  );
}