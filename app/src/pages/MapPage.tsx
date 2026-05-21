import { useEffect, useState } from "react";
import { MapContainer, Marker, Popup, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

import Station, { type StationData } from "../components/Station";
import { useAuth } from "../components/AuthContext";
import { type VehicleData } from "../components/Vehicle";
import { ACME_API_URL } from "../App";

const defaultIcon = L.icon({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

export default function MapPage() {
  const [stations, setStations] = useState<StationData[]>([]);
  const [vehiclesByStation, setVehiclesByStation] = useState<
    Record<string, VehicleData[]>
  >({});
  const [loadingStationId, setLoadingStationId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { user } = useAuth();


  useEffect(() => {
    loadStations();
  }, []);


  async function reserveVehicle(vehicle: VehicleData) {
    if (!user) {
      return;
    }

    try {
      const response = await fetch(`${ACME_API_URL}/reservations`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${user.id}`,
        },
        body: JSON.stringify({
          userId: user.id,
          vehicleId: vehicle.id,
          stationId: vehicle.stationId,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      const requestId = data.requestId;

      const source = new EventSource(
        `${ACME_API_URL}/reservations/${requestId}/events`
      );

      source.addEventListener("reservation-update", async (event) => {
        const update = JSON.parse(event.data);

        console.log("Reservation update:", update);

        if (update.status === "RESERVED") {
          source.close();

          await refreshVehicles(vehicle.stationId);

          alert("Prenotazione confermata");
        }

        if (update.status === "FAILED") {
          source.close();

          alert(update.message ?? "Prenotazione fallita");
        }
      });

      source.onerror = (err) => {
        console.error("SSE error:", err);
        source.close();
      };
    } catch (err) {
      console.error(err);
      alert("Errore durante la prenotazione");
    }
  }

  async function refreshVehicles(stationId: string) {
    try {
      const response = await fetch(
        `${ACME_API_URL}/vehicles?stationId=${stationId}`
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const vehicles: VehicleData[] = await response.json();

      setVehiclesByStation((prev) => ({
        ...prev,
        [stationId]: vehicles,
      }));
    } catch (err) {
      console.error(err);
      setError("Impossibile aggiornare i veicoli della stazione");
    }
  }

  async function loadStations() {
    try {
      const response = await fetch(`${ACME_API_URL}/stations`);

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data: StationData[] = await response.json();
      setStations(data);
    } catch (err) {
      console.error(err);
      setError("Impossibile caricare le stazioni");
    }
  }

  async function loadVehicles(stationId: string) {
    if (vehiclesByStation[stationId]) {
      return;
    }

    try {
      setLoadingStationId(stationId);

      const response = await fetch(
        `${ACME_API_URL}/vehicles?stationId=${stationId}`
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const vehicles: VehicleData[] = await response.json();

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