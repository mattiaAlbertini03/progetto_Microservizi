import { useCallback, useEffect, useRef, useState } from "react";

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
            <p style={{ padding: "1.5rem" }}>
                Caricamento prenotazioni...
            </p>
        );
    }

    if (error) {
        return (
            <p style={{ padding: "1.5rem", color: "red" }}>
                {error}
            </p>
        );
    }

    return (
        <main style={{ padding: "1.5rem" }}>
            <h1>Prenotazioni</h1>

            {reservations.length === 0 ? (
                <p>Nessuna prenotazione trovata.</p>
            ) : (
                <div
                    style={{
                        display: "grid",
                        gap: "1rem",
                    }}
                >
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
