import type { ReservationData } from "../types/domain";

type Props = {
    reservation: ReservationData;
    onPickup: (
        reservation: ReservationData
    ) => void | Promise<void>;
    onCancel: (
        reservation: ReservationData
    ) => void | Promise<void>;
    onReturn: (
        reservation: ReservationData
    ) => void | Promise<void>;
    onTrack: (
        reservation: ReservationData
    ) => void;
};

export default function ReservationCard({
    reservation,
    onPickup,
    onCancel,
    onReturn,
    onTrack,
}: Props) {
    const canCancel =
        reservation.status === "PENDING" ||
        reservation.status === "RESERVED";

    const canPickup = reservation.status === "RESERVED";
    const isPickedUp =
        reservation.status === "ACTIVE" ||
        reservation.status === "PICKED_UP";
    const canReturn = isPickedUp;
    const canTrack = isPickedUp;
    const scheduledStart = reservation.scheduledStartEpochMs
        ? new Date(reservation.scheduledStartEpochMs).toLocaleString("it-IT")
        : null;

    return (
        <div
            style={{
                padding: "1rem",
                border: "1px solid #ddd",
                borderRadius: "8px",
                background: "white",
            }}
        >
            <strong>{reservation.reservationId}</strong>

            <p>Veicolo: {reservation.vehicleId}</p>
            <p>Stazione: {reservation.stationId}</p>
            {scheduledStart && <p>Inizio previsto: {scheduledStart}</p>}
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

                {canTrack && (
                    <button onClick={() => onTrack(reservation)}>
                        Traccia posizione
                    </button>
                )}
            </div>
        </div>
    );
}
