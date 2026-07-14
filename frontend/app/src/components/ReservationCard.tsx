import type { ReservationData } from "../types/domain";

type Props = {
    reservation: ReservationData;
    onPickup: (reservation: ReservationData) => void | Promise<void>;
    onCancel: (reservation: ReservationData) => void | Promise<void>;
    onReturn: (reservation: ReservationData) => void | Promise<void>;
};

export default function ReservationCard({
    reservation,
    onPickup,
    onCancel,
    onReturn,
}: Props) {
    const canCancel =
        reservation.status === "PENDING" ||
        reservation.status === "RESERVED";

    const canPickup = reservation.status === "RESERVED";
    const canReturn = reservation.status === "ACTIVE";

    return (
        <div
            style={{
                padding: "1rem",
                border: "1px solid #ddd",
                borderRadius: "8px",
                background: "white",
            }}
        >
            <strong>{reservation.id}</strong>

            <p>Veicolo: {reservation.vehicleId}</p>
            <p>Stazione: {reservation.stationId}</p>
            <p>Stato: {reservation.status}</p>

            <div style={{ display: "flex", gap: "0.5rem" }}>
                {canPickup && (
                    <button onClick={() => onPickup(reservation)}>
                        Pickup
                    </button>
                )}

                {canCancel && (
                    <button onClick={() => onCancel(reservation)}>
                        Annulla
                    </button>
                )}

                {canReturn && (
                    <button onClick={() => onReturn(reservation)}>
                        Riconsegna
                    </button>
                )}
            </div>
        </div>
    );
}