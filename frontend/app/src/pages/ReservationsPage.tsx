import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { useAuth } from "../components/AuthContext";
import ReservationCard from "../components/ReservationCard";
import {
    getReservations,
    sendReservationAction,
} from "../api/acmeApi";
import type { ReservationData } from "../types/domain";
import { useReservationEvents } from "../components/ReservationEventsContext";
import { SIMULATION_VISUALIZER_URL } from "../App";

export default function ReservationsPage() {
    const { user } = useAuth();

    const [reservations, setReservations] = useState<ReservationData[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const refreshTimers = useRef<number[]>([]);

    const { listenReservationEvents } = useReservationEvents();

    const loadReservations = useCallback(async (showLoading = false) => {
        if (!user) {
            setReservations([]);
            setLoading(false);
            return;
        }

        try {
            if (showLoading) {
                setLoading(true);
            }

            const data = await getReservations(user);

            setReservations(data);
            setError(null);
        } catch (err) {
            console.error("Errore caricamento prenotazioni:", err);

            const message =
                err instanceof Error
                    ? err.message
                    : "Impossibile caricare le prenotazioni";

            setError(message);
        } finally {
            setLoading(false);
        }
    }, [user]);

    useEffect(() => {
        void loadReservations(true);
    }, [loadReservations]);

    useEffect(() => {
        return () => {
            refreshTimers.current.forEach(window.clearTimeout);
        };
    }, []);

    useEffect(() => {
        const handleReservationsChanged = () => {
            void loadReservations();
        };

        window.addEventListener(
            "reservations-changed",
            handleReservationsChanged
        );

        window.addEventListener(
            "reservation-confirmed",
            handleReservationsChanged
        );

        return () => {
            window.removeEventListener(
                "reservations-changed",
                handleReservationsChanged
            );

            window.removeEventListener(
                "reservation-confirmed",
                handleReservationsChanged
            );
        };
    }, [loadReservations]);

    function trackReservation(
        reservation: ReservationData
    ): void {
        if (!SIMULATION_VISUALIZER_URL) {
            alert("Il visualizzatore della simulazione non è configurato.");
            return;
        }

        const params = new URLSearchParams({
            reservationId: reservation.reservationId,
            vehicleId: reservation.vehicleId,
        });

        window.open(
            `${SIMULATION_VISUALIZER_URL}/?${params.toString()}`,
            "_blank",
            "noopener,noreferrer"
        );
    }

    async function pickupReservation(
        reservation: ReservationData
    ): Promise<void> {
        let eventSource: EventSource;

        try {
            eventSource = await listenReservationEvents(
                reservation.reservationId
            );

            const sent = await executeReservationAction(
                reservation,
                "pickup",
                "Pickup richiesto"
            );

            if (!sent) {
                eventSource.close();
            }
        } catch (err) {
            console.error("Errore notifiche prenotazione:", err);

            const message =
                err instanceof Error
                    ? err.message
                    : "Impossibile ascoltare le notifiche della prenotazione.";

            alert(message);
        }
    }

    async function cancelReservation(
        reservation: ReservationData
    ): Promise<void> {
        await executeReservationAction(
            reservation,
            "cancel",
            "Annullamento richiesto"
        );
    }

    async function returnReservation(
        reservation: ReservationData
    ): Promise<void> {
        let eventSource: EventSource;

        try {
            eventSource = await listenReservationEvents(
                reservation.reservationId
            );

            const sent = await executeReservationAction(
                reservation,
                "return",
                "Richiesta di riconsegna inviata"
            );

            if (!sent) {
                eventSource.close();
            }
        } catch (err) {
            console.error("Errore notifiche riconsegna:", err);

            const message =
                err instanceof Error
                    ? err.message
                    : "Impossibile ascoltare le notifiche di riconsegna.";

            alert(message);
        }
    }

    async function executeReservationAction(
        reservation: ReservationData,
        action: "pickup" | "cancel" | "return",
        successMessage: string
    ): Promise<boolean> {
        if (!user) {
            alert("Devi effettuare il login.");
            return false;
        }

        try {
            await sendReservationAction(
                user,
                reservation.reservationId,
                action
            );

            alert(successMessage);

            if (action !== "return") {
                await loadReservations();
                scheduleReservationRefreshes();
            }

            return true;
        } catch (err) {
            console.error("Errore operazione prenotazione:", err);

            const message =
                err instanceof Error
                    ? err.message
                    : "Errore durante l'operazione";

            if (
                message === "FORBIDDEN" ||
                message.startsWith("FORBIDDEN:")
            ) {
                alert(
                    "Non sei autorizzato a modificare questa prenotazione."
                );
                return false;
            }

            if (
                message === "CONFLICT" ||
                message.startsWith("CONFLICT:")
            ) {
                alert(
                    "Operazione non consentita nello stato attuale della prenotazione."
                );
                return false;
            }

            if (
                message.includes("CAMUNDA_UNAVAILABLE") ||
                message.includes("Unable to start")
            ) {
                alert(
                    "Il servizio di orchestrazione Camunda non è disponibile."
                );
                return false;
            }

            alert(message);
            return false;
        }
    }

    function scheduleReservationRefreshes(): void {
        refreshTimers.current.forEach(window.clearTimeout);
        refreshTimers.current = [750, 2000, 5000].map((delay) =>
            window.setTimeout(() => {
                void loadReservations();
            }, delay)
        );
    }

    if (loading) {
        return (
            <main className="page">
                <div className="state-panel panel"><span className="spinner" /><strong>Caricamento prenotazioni…</strong></div>
            </main>
        );
    }

    if (error) {
        return (
            <main className="page"><div className="alert">{error}</div></main>
        );
    }

    const activeCount = reservations.filter((reservation) =>
        reservation.status === "ACTIVE" || reservation.status === "PICKED_UP"
    ).length;
    const upcomingCount = reservations.filter((reservation) =>
        reservation.status === "RESERVED" || reservation.status === "PENDING"
    ).length;

    return (
        <main className="page reservations-page">
            <header className="page-header">
                <div>
                    <p className="eyebrow">I tuoi viaggi</p>
                    <h1 className="page-title">Prenotazioni</h1>
                    <p className="page-subtitle">Controlla i prossimi ritiri e segui in tempo reale le corse attive.</p>
                </div>
                <div className="reservation-summary">
                    <span><strong>{activeCount}</strong> attive</span>
                    <span><strong>{upcomingCount}</strong> in arrivo</span>
                </div>
            </header>

            {reservations.length === 0 ? (
                <div className="state-panel panel">
                    <div className="state-icon">⌖</div>
                    <h2>Nessun viaggio pianificato</h2>
                    <p className="page-subtitle">Apri la mappa, scegli una stazione e prenota il tuo prossimo mezzo.</p>
                    <Link className="button" to="/">Esplora la mappa</Link>
                </div>
            ) : (
                <div className="reservation-grid">
                    {reservations.map((reservation) => (
                        <ReservationCard
                            key={reservation.reservationId}
                            reservation={reservation}
                            onPickup={pickupReservation}
                            onCancel={cancelReservation}
                            onReturn={returnReservation}
                            onTrack={trackReservation}
                        />
                    ))}
                </div>
            )}
        </main>
    );
}
