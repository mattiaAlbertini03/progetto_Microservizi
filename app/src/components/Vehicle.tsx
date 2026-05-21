export type VehicleData = {
    id: string;
    type: "CAR" | "SCOOTER" | "KICKSCOOTER" | string;
    stationId: string;
    status: string;
    batteryPct: number;
    qrCode?: string;
};

type VehicleProps = {
    vehicle: VehicleData;
    onReserve?: (vehicle: VehicleData) => void | Promise<void>;
};

export default function Vehicle({ vehicle, onReserve }: VehicleProps) {
    const iconByType: Record<string, string> = {
        CAR: "🚗",
        SCOOTER: "🛵",
        KICKSCOOTER: "🛴",
    };

    const canReserve = vehicle.status === "AVAILABLE" && !!onReserve;

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