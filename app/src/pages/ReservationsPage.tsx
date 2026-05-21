import { useEffect, useState } from "react";

import { ACME_API_URL } from "../App";
import { useAuth } from "../components/AuthContext";
import ReservationCard, {
    type ReservationData,
} from "../components/ReservationCard";

export default function ReservationsPage() {
    const { user } = useAuth();

    const [reservations, setReservations] = useState<ReservationData[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadReservations();
    }, []);

    async function loadReservations() {
        if (!user) {
            return;
        }

        try {
            setLoading(true);

            const response = await fetch(
                `${ACME_API_URL}/reservations?userId=${user.id}`,
                {
                    headers: {
                        Authorization: `Bearer ${user.id}`,
                    },
                }
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data: ReservationData[] = await response.json();
            setReservations(data);
        } catch (err) {
            console.error(err);
            setError("Impossibile caricare le prenotazioni");
        } finally {
            setLoading(false);
        }
    }

    async function pickupReservation(reservation: ReservationData) {
        await sendReservationAction(
            reservation,
            "pickup",
            "Pickup richiesto"
        );
    }

    async function cancelReservation(reservation: ReservationData) {
        await sendReservationAction(
            reservation,
            "cancel",
            "Annullamento richiesto"
        );
    }

    async function returnReservation(reservation: ReservationData) {
        await sendReservationAction(
            reservation,
            "return",
            "Riconsegna richiesta"
        );
    }

    async function sendReservationAction(
        reservation: ReservationData,
        action: "pickup" | "cancel" | "return",
        successMessage: string
    ) {
        if (!user) {
            return;
        }

        try {
            const response = await fetch(
                `${ACME_API_URL}/reservations/${reservation.id}/${action}`,
                {
                    method: "POST",
                    headers: {
                        Authorization: `Bearer ${user.id}`,
                    },
                }
            );

            if (!response.ok) {
                if (response.status === 409) {
                    if (action === "return") {
                        alert("Il veicolo non è ancora arrivato alla stazione di destinazione.");
                    } else {
                        alert("Operazione non consentita nello stato attuale della prenotazione.");
                    }

                    return;
                }

                if (response.status === 403) {
                    alert("Non sei autorizzato a modificare questa prenotazione.");
                    return;
                }

                throw new Error(`HTTP ${response.status}`);
            }

            alert(successMessage);

            await loadReservations();
        } catch (err) {
            console.error(err);
            alert("Errore durante l'operazione");
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