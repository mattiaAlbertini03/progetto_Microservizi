type VehicleData = {
    id: string;
    type: "CAR" | "SCOOTER" | "KICKSCOOTER" | string;
    stationId: string;
    status: string;
    batteryPct: number;
    qrCode?: string;
};

type VehicleProps = {
    vehicle: VehicleData;
    onReserve: (vehicle: VehicleData) => void | Promise<void>;
};

export default function Vehicle({ vehicle, onReserve }: VehicleProps) {
    const iconByType: Record<string, string> = {
        CAR: "🚗",
        SCOOTER: "🛵",
        KICKSCOOTER: "🛴",
    };

    return (
        <div
            style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "0.5rem",
                padding: "0.4rem 0",
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
                    Batteria: {vehicle.batteryPct}% — Stato: {vehicle.status}
                </div>
            </div>

            <button
                disabled={vehicle.status !== "AVAILABLE"}
                onClick={() => onReserve(vehicle)}
                style={{
                    cursor: vehicle.status === "AVAILABLE" ? "pointer" : "not-allowed",
                    padding: "0.3rem 0.5rem",
                }}
            >
                Prenota
            </button>
        </div>
    );
}