import type { ReservationData } from "../types/domain";

type Props = {
    reservation: ReservationData;
    onPickup: (reservation: ReservationData) => void | Promise<void>;
    onCancel: (reservation: ReservationData) => void | Promise<void>;
    onReturn: (reservation: ReservationData) => void | Promise<void>;
    onTrack: (reservation: ReservationData) => void;
};

const statusLabels: Record<string, string> = {
    PENDING: "In elaborazione",
    RESERVED: "Prenotata",
    ACTIVE: "Corsa attiva",
    PICKED_UP: "Corsa attiva",
    COMPLETED: "Completata",
    CANCELLED: "Annullata",
    NO_SHOW: "Mancato ritiro",
};

function statusTone(status: string) {
    if (status === "ACTIVE" || status === "PICKED_UP") return "active";
    if (status === "RESERVED" || status === "PENDING") return "upcoming";
    if (status === "CANCELLED" || status === "NO_SHOW") return "cancelled";
    return "complete";
}

export default function ReservationCard({
    reservation,
    onPickup,
    onCancel,
    onReturn,
    onTrack,
}: Props) {
    const canCancel = reservation.status === "PENDING" || reservation.status === "RESERVED";
    const canPickup = reservation.status === "RESERVED";
    const isPickedUp = reservation.status === "ACTIVE" || reservation.status === "PICKED_UP";
    const scheduledStart = reservation.scheduledStartEpochMs
        ? new Date(reservation.scheduledStartEpochMs)
        : null;

    return (
        <article className="reservation-card panel">
            <div className="reservation-card-top">
                <div className="reservation-vehicle-icon">🚙</div>
                <div className="reservation-identity">
                    <span className={`reservation-status reservation-status--${statusTone(reservation.status)}`}>
                        {statusLabels[reservation.status] ?? reservation.status}
                    </span>
                    <h2>{reservation.vehicleId}</h2>
                    <p>Stazione {reservation.stationId}</p>
                </div>
            </div>

            <div className="reservation-timeline">
                <div>
                    <span>Partenza</span>
                    <strong>
                        {scheduledStart
                            ? scheduledStart.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })
                            : "Immediata"}
                    </strong>
                    <small>
                        {scheduledStart
                            ? scheduledStart.toLocaleDateString("it-IT", { day: "2-digit", month: "short" })
                            : "Oggi"}
                    </small>
                </div>
                <span className="timeline-line"><i /></span>
                <div>
                    <span>Prenotazione</span>
                    <strong className="reservation-code">#{reservation.reservationId.slice(0, 8)}</strong>
                    <small>ACME pass</small>
                </div>
            </div>

            <div className="reservation-actions">
                {canPickup && (
                    <button className="button" onClick={() => onPickup(reservation)}>
                        Inizia corsa
                    </button>
                )}
                {isPickedUp && (
                    <button className="button" onClick={() => onReturn(reservation)}>
                        Riconsegna
                    </button>
                )}
                {isPickedUp && (
                    <button className="button button--secondary" onClick={() => onTrack(reservation)}>
                        Traccia live
                    </button>
                )}
                {canCancel && (
                    <button className="button button--danger" onClick={() => onCancel(reservation)}>
                        Annulla
                    </button>
                )}
            </div>
        </article>
    );
}
