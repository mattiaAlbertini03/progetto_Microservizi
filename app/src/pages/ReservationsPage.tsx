import { useEffect, useState } from "react";

import { ACME_API_URL } from "../App";
import { useAuth } from "../components/AuthContext";
import ReservationCard from "../components/ReservationCard";
import {
    getReservations,
    sendReservationAction,
} from "../api/acmeApi";
import type { ReservationData } from "../types/domain";

export default function ReservationsPage() {
    const { user } = useAuth();

    const [reservations, setReservations] = useState<ReservationData[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadReservations();
    }, [user]);

    async function loadReservations() {
        if (!user) {
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            const data = await getReservations(user.id);
            setReservations(data);
        } catch (err) {
            console.error(err);
            setError("Impossibile caricare le prenotazioni");
        } finally {
            setLoading(false);
        }
    }

    function listenReturnEvents(reservationId: string) {
        const eventSource = new EventSource(
            `${ACME_API_URL}/reservations/${reservationId}/return/events`
        );

        eventSource.addEventListener("reservation-update", async (event) => {
            const data = JSON.parse((event as MessageEvent).data);

            switch (data.status) {
                case "VEHICLE_NOT_AT_DESTINATION":
                    alert("Il veicolo non è ancora arrivato alla stazione.");
                    break;

                case "RETURN_CONFIRMED":
                    alert("Veicolo riconsegnato correttamente.");
                    await loadReservations();
                    eventSource.close();
                    break;

                case "RETURN_FAILED":
                    alert("Errore durante la riconsegna.");
                    eventSource.close();
                    break;
            }
        });

        eventSource.onerror = () => {
            console.error("Errore SSE return");
            eventSource.close();
        };

        return eventSource;
    }

    async function pickupReservation(reservation: ReservationData) {
        await executeReservationAction(
            reservation,
            "pickup",
            "Pickup richiesto"
        );
    }

    async function cancelReservation(reservation: ReservationData) {
        await executeReservationAction(
            reservation,
            "cancel",
            "Annullamento richiesto"
        );
    }

    async function returnReservation(reservation: ReservationData) {
        const eventSource = listenReturnEvents(reservation.id);

        try {
            await executeReservationAction(
                reservation,
                "return",
                "Richiesta di riconsegna inviata"
            );
        } catch (err) {
            eventSource.close();
            throw err;
        }
    }

    async function executeReservationAction(
        reservation: ReservationData,
        action: "pickup" | "cancel" | "return",
        successMessage: string
    ) {
        if (!user) {
            return;
        }

        try {
            await sendReservationAction(user.id, reservation.id, action);

            alert(successMessage);

            if (action !== "return") {
                await loadReservations();
            }
        } catch (err) {
            console.error(err);

            if (err instanceof Error && err.message === "FORBIDDEN") {
                alert("Non sei autorizzato a modificare questa prenotazione.");
                return;
            }

            if (err instanceof Error && err.message === "CONFLICT") {
                alert("Operazione non consentita nello stato attuale della prenotazione.");
                return;
            }

            alert("Errore durante l'operazione");
            throw err;
        }
    }

    if (loading) {
        return <p style={{ padding: "1.5rem" }}>Caricamento prenotazioni...</p>;
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
                <div style={{ display: "grid", gap: "1rem" }}>
                    {reservations.map((reservation) => (
                        <ReservationCard
                            key={reservation.id}
                            reservation={reservation}
                            onPickup={pickupReservation}
                            onCancel={cancelReservation}
                            onReturn={returnReservation}
                        />
                    ))}
                </div>
            )}
        </main>
    );
}