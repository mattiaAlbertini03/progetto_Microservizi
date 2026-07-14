import type { VehicleData } from "../types/domain";

type VehicleProps = {
    vehicle: VehicleData;
    onReserve?: (vehicle: VehicleData) => void | Promise<void>;
};

function canReserveVehicle(vehicle: VehicleData) {
    return vehicle.status === "AVAILABLE"
        || (vehicle.status === "CHARGING" && vehicle.batteryPct >= 80);
}

export default function Vehicle({ vehicle, onReserve }: VehicleProps) {
    const iconByType: Record<string, string> = {
        CAR: "🚗",
        SCOOTER: "🛵",
        KICKSCOOTER: "🛴",
    };

    const canReserve = !!onReserve && canReserveVehicle(vehicle);

    return (
        <div
            style={{
                display: "flex",
                justifyContent: "space-between",
                gap: "0.5rem",
                padding: "0.5rem 0",
                borderBottom: "1px solid #ddd",
            }}
        >
            <div>
                <div>
                    <span style={{ marginRight: "0.4rem" }}>
                        {iconByType[vehicle.type] ?? "🚘"}
                    </span>
                    <strong>{vehicle.id}</strong>
                </div>

                <div style={{ fontSize: "0.8rem", color: "#555" }}>
                    Tipo: {vehicle.type}
                </div>

                <div style={{ fontSize: "0.8rem", color: "#555" }}>
                    Batteria: {vehicle.batteryPct}%
                </div>

                <div style={{ fontSize: "0.8rem", color: "#555" }}>
                    Stato: {vehicle.status}
                </div>
            </div>

            {onReserve && (
                <button
                    disabled={!canReserve}
                    onClick={() => onReserve(vehicle)}
                >
                    Prenota
                </button>
            )}
        </div>
    );
}