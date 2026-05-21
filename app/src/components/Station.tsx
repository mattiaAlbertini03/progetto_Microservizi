import Vehicle, { type VehicleData } from "./Vehicle";

export type StationData = {
    id: string;
    name: string;
    lat: number;
    lon: number;
    capacity: number;
};

type StationProps = {
    station: StationData;
    vehicles?: VehicleData[];
    loading?: boolean;
    onReserve?: (vehicle: VehicleData) => void | Promise<void>;
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
                <p>Nessun veicolo presente.</p>
            )}

            {!loading &&
                vehicles?.map((vehicle) => (
                    <Vehicle
                        key={vehicle.id}
                        vehicle={vehicle}
                        onReserve={onReserve}
                    />
                ))}
        </div>
    );
}