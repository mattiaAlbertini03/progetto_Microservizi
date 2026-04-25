import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import { useAuth } from "./AuthContext";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import Vehicle from "./Vehicle";

const defaultIcon = L.icon({
    iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
});

const ACME_API_URL = "http://192.168.49.2:30080";

export default function Map() {
    const [stations, setStations] = useState([]);
    const [vehiclesByStation, setVehiclesByStation] = useState({});
    const [loadingStationId, setLoadingStationId] = useState(null);
    const { user } = useAuth();
    const [error, setError] = useState(null);

    useEffect(() => {
        fetch(`${ACME_API_URL}/stations`)
            .then((res) => {
                if (!res.ok) {
                    throw new Error(`Errore HTTP ${res.status}`);
                }
                return res.json();
            })
            .then((data) => setStations(data))
            .catch((err) => {
                console.error(err);
                setError("Impossibile caricare le stazioni");
            });
    }, []);

    async function loadVehicles(stationId) {
        if (vehiclesByStation[stationId]) {
            return;
        }

        try {
            setLoadingStationId(stationId);

            const res = await fetch(`${ACME_API_URL}/vehicles?stationId=${stationId}`);

            if (!res.ok) {
                throw new Error(`Errore HTTP ${res.status}`);
            }

            const vehicles = await res.json();

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

    async function reserveVehicle(vehicle) {
        if (!user) {
            alert("Devi effettuare il login per prenotare un veicolo.");
            return;
        }

        try {
            const res = await fetch(`${ACME_API_URL}/reservations`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    userId: user.id,
                    vehicleId: vehicle.id,
                    stationId: vehicle.stationId,
                }),
            });

            if (!res.ok) {
                throw new Error(`Errore HTTP ${res.status}`);
            }

            const data = await res.json();
            const reservationId = data.reservationId;

            const source = new EventSource(
                `${ACME_API_URL}/reservations/${reservationId}/events`
            );

            source.addEventListener("reservation-update", (event) => {
                const update = JSON.parse(event.data);

                console.log("Reservation update:", update);

                alert(update.message);

                if (update.status === "RESERVED") {
                    setVehiclesByStation((prev) => ({
                        ...prev,
                        [vehicle.stationId]: prev[vehicle.stationId].map((v) =>
                            v.id === vehicle.id
                                ? { ...v, status: "RESERVED" }
                                : v
                        ),
                    }));

                    source.close();
                }

                if (update.status === "FAILED") {
                    source.close();
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

    return (
        <div style={{ height: "100vh", width: "100%" }}>
            {error && (
                <div style={{ padding: "1rem", color: "red" }}>
                    {error}
                </div>
            )}

            <MapContainer
                center={[44.4949, 11.3426]}
                zoom={14}
                style={{ height: "100%", width: "100%" }}
            >
                <TileLayer
                    attribution='&copy; OpenStreetMap contributors'
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
                        <Popup minWidth={280}>
                            <strong>{station.name}</strong>

                            <hr />

                            <strong>Veicoli disponibili</strong>

                            {loadingStationId === station.id && (
                                <p>Caricamento veicoli...</p>
                            )}

                            {!loadingStationId &&
                                vehiclesByStation[station.id]?.length === 0 && (
                                    <p>Nessun veicolo presente.</p>
                                )}

                            {vehiclesByStation[station.id]?.map((vehicle) => (
                                <Vehicle
                                    key={vehicle.id}
                                    vehicle={vehicle}
                                    onReserve={reserveVehicle}
                                />
                            ))}
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>
        </div>
    );
}