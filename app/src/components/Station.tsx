import Vehicle from "./Vehicle";
import type { StationData, VehicleData } from "../types/domain";

type StationProps = {
    station: StationData;
    vehicles?: VehicleData[];
    loading?: boolean;
    onReserve?: (station: StationData, vehicle: VehicleData) => void | Promise<void>;
};

export default function Station({
    station,
    vehicles,
    loading = false,
    onReserve,
}: StationProps) {
    return (
        <div>
            <strong>{station.name}</strong>

            <div style={{ fontSize: "0.85rem", color: "#555", marginTop: "0.25rem" }}>
                Capacità: {station.capacity}
            </div>

            <hr />

            <strong>Veicoli</strong>

            {loading && <p>Caricamento veicoli...</p>}

            {!loading && vehicles?.length === 0 && (
                <p>Nessun veicolo disponibile.</p>
            )}

            {!loading &&
                vehicles?.map((vehicle) => (
                    <Vehicle
                        key={vehicle.id}
                        vehicle={vehicle}
                        onReserve={
                            onReserve
                                ? () => onReserve(station, vehicle)
                                : undefined
                        }
                    />
                ))}
        </div>
    );
}