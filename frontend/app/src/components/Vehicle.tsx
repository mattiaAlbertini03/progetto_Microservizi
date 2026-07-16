import type { VehicleData } from "../types/domain";

type VehicleProps = {
    vehicle: VehicleData;
    onReserve?: (vehicle: VehicleData) => void | Promise<void>;
    onQrPickup?: (vehicle: VehicleData) => void | Promise<void>;
};

function canReserveVehicle(vehicle: VehicleData) {
    return vehicle.status === "AVAILABLE"
        || (vehicle.status === "CHARGING" && vehicle.batteryPct >= 80);
}

export default function Vehicle({ vehicle, onReserve, onQrPickup }: VehicleProps) {
    const iconByType: Record<string, string> = {
        CAR: "🚗",
        SCOOTER: "🛵",
        KICKSCOOTER: "🛴",
    };

    const canReserve = !!onReserve && canReserveVehicle(vehicle);
    const statusLabels: Record<string, string> = {
        AVAILABLE: "Disponibile",
        CHARGING: "In ricarica",
        ON_HOLD: "Prenotato",
        IN_USE: "In uso",
        MAINTENANCE: "Manutenzione",
        UNAVAILABLE: "Non disponibile",
    };
    const typeLabels: Record<string, string> = {
        CAR: "Auto elettrica",
        SCOOTER: "Scooter elettrico",
        KICKSCOOTER: "Monopattino",
    };
    const batteryTone = vehicle.batteryPct < 20 ? "low" : vehicle.batteryPct < 55 ? "medium" : "high";

    return (
        <article className="vehicle-card">
            <div className="vehicle-summary">
                <div className="vehicle-icon">{iconByType[vehicle.type] ?? "🚘"}</div>
                <div className="vehicle-identity">
                    <div>
                        <strong>{vehicle.id}</strong>
                        <span className={`status-badge status-badge--${canReserveVehicle(vehicle) ? "available" : "busy"}`}>
                            {statusLabels[vehicle.status] ?? vehicle.status}
                        </span>
                    </div>
                    <small>{typeLabels[vehicle.type] ?? vehicle.type}</small>
                    <div className="battery-row">
                        <span className={`battery-icon battery-icon--${batteryTone}`}><i /></span>
                        <div className="battery-track"><i style={{ width: `${Math.max(0, Math.min(100, vehicle.batteryPct))}%` }} /></div>
                        <strong>{Math.round(vehicle.batteryPct)}%</strong>
                    </div>
                </div>
            </div>

            <div className="vehicle-actions">
                {onReserve && (
                    <button
                        className="button"
                        disabled={!canReserve}
                        onClick={() => onReserve(vehicle)}
                    >
                        Prenota
                    </button>
                )}

                {onQrPickup && (
                    <button
                        className="button button--secondary"
                        disabled={!canReserve}
                        onClick={() => onQrPickup(vehicle)}
                    >
                        QR rapido
                    </button>
                )}
            </div>
        </article>
    );
}
