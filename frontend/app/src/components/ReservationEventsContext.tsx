import {
    createContext,
    useContext,
    useEffect,
    useRef,
    type ReactNode,
} from "react";

import { ACME_API_URL } from "../App";
import { useNotifications } from "./NotificationsContext";
import { showBrowserNotification } from "../browserNotifications";

type ReservationEventsContextType = {
    listenReservationRequest: (requestId: string, stationId?: string) => void;
    listenReservationEvents: (reservationId: string) => Promise<EventSource>;
};

const ReservationEventsContext =
    createContext<ReservationEventsContextType | null>(null);

const PARKED_REMINDER_INTERVAL_MS = 60_000;

export function ReservationEventsProvider({ children }: { children: ReactNode }) {
    const sources = useRef<Record<string, EventSource>>({});
    const parkedReminderIntervals = useRef<Record<string, number>>({});
    const notify = useNotifications();

    useEffect(() => {
        return () => {
            Object.values(sources.current).forEach((source) => source.close());
            Object.values(parkedReminderIntervals.current).forEach(window.clearInterval);
            sources.current = {};
            parkedReminderIntervals.current = {};
        };
    }, []);

    function listenReservationRequest(requestId: string, stationId?: string) {
        const key = `request-${requestId}`;

        if (sources.current[key]) {
            return;
        }

        const source = new EventSource(
            `${ACME_API_URL}/reservation-requests/${requestId}/events`
        );

        sources.current[key] = source;

        source.addEventListener("reservation-update", (event) => {
            const data = JSON.parse((event as MessageEvent).data);

            switch (data.status) {
                case "BANK_CONNECTION_REQUIRED":
                    localStorage.setItem("acme-pending-bank-request", requestId);
                    notify(data.message ?? "Collega il conto dal profilo per continuare.", "info");
                    window.dispatchEvent(new CustomEvent("bank-connection-required", {
                        detail: { requestId },
                    }));
                    break;

                case "RESERVED":
                    localStorage.removeItem("acme-pending-bank-request");
                    notify("Prenotazione confermata", "success");

                    if (data.reservationId) {
                        void listenReservationEvents(data.reservationId).catch(
                            (error) => {
                                console.error(
                                    "Reservation notification SSE error:",
                                    error
                                );
                            }
                        );
                    }

                    window.dispatchEvent(
                        new CustomEvent("reservation-confirmed", {
                            detail: {
                                requestId,
                                stationId,
                                reservationId: data.reservationId,
                            },
                        })
                    );

                    source.close();
                    delete sources.current[key];
                    break;

                case "FAILED":
                    notify(data.message ?? "Prenotazione fallita", "error");

                    source.close();
                    delete sources.current[key];
                    break;
            }
        });

        source.onerror = (err) => {
            console.error("Reservation request SSE error:", err);
        };
    }

    async function listenReservationEvents(reservationId: string) {
        const key = `reservation-${reservationId}`;
        const existingSource = sources.current[key];

        if (existingSource?.readyState === EventSource.CLOSED) {
            delete sources.current[key];
        } else if (existingSource) {
            await waitUntilOpen(existingSource);
            return existingSource;
        }

        const eventSource = new EventSource(
            `${ACME_API_URL}/reservations/${reservationId}/events`
        );

        sources.current[key] = eventSource;

        eventSource.addEventListener("reservation-update", (event) => {
            const data = JSON.parse((event as MessageEvent).data);

            switch (data.status) {
                case "DEPOSIT_HELD":
                    notify(
                        data.message ??
                            "Il deposito è stato trattenuto con successo.", "success"
                    );
                    break;

                case "RETURN_REQUEST_ACCEPTED":
                    notify(data.message ?? "Richiesta di riconsegna accettata.", "success");
                    break;

                case "VEHICLE_AT_STATION":
                    if (data.movementStatus !== "STOPPED") {
                        window.clearInterval(parkedReminderIntervals.current[reservationId]);
                        delete parkedReminderIntervals.current[reservationId];
                        break;
                    }
                    if (!parkedReminderIntervals.current[reservationId]) {
                        parkedReminderIntervals.current[reservationId] = window.setInterval(() => {
                            const message = "Il veicolo è parcheggiato presso la stazione di destinazione. Ricordati di completare la riconsegna.";
                            showBrowserNotification("Riconsegna ACMEMobility", message);
                            notify(message, "info");
                        }, PARKED_REMINDER_INTERVAL_MS);
                    }
                    break;

                case "VEHICLE_NOT_AT_STATION":
                    window.clearInterval(parkedReminderIntervals.current[reservationId]);
                    delete parkedReminderIntervals.current[reservationId];
                    notify(data.message, "error");
                    break;

                case "LOW_BATTERY_PENALTY_APPLIED":
                    notify(
                        data.message ??
                            "Batteria sotto il 15%: applicata una penale del 10%.", "error"
                    );
                    break;

                case "DEPOSIT_RELEASED":
                    notify(
                        data.message ??
                            "Il deposito è stato rilasciato con successo.", "success"
                    );
                    break;

                case "NOT_ENOUGH_FUNDS":
                    localStorage.setItem("acme-pending-recharge", reservationId);
                    notify(
                        data.message ?? "Saldo insufficiente. Ricarica il conto dal profilo.",
                        "error"
                    );
                    window.dispatchEvent(new CustomEvent("bank-recharge-required", {
                        detail: { reservationId },
                    }));
                    break;

                case "RETURN_CONFIRMED":
                    localStorage.removeItem("acme-pending-recharge");
                    window.clearInterval(parkedReminderIntervals.current[reservationId]);
                    delete parkedReminderIntervals.current[reservationId];
                    notify(data.message, "success");

                    window.dispatchEvent(
                        new CustomEvent("reservations-changed")
                    );

                    eventSource.close();
                    delete sources.current[key];
                    break;

                case "RETURN_FAILED":
                    notify(data.message, "error");

                    eventSource.close();
                    delete sources.current[key];
                    break;
            }
        });

        eventSource.onerror = () => {
            console.error("Errore SSE return");
        };

        try {
            await waitUntilOpen(eventSource);
            return eventSource;
        } catch (error) {
            eventSource.close();
            delete sources.current[key];
            throw error;
        }
    }

    return (
        <ReservationEventsContext.Provider
            value={{
                listenReservationRequest,
                listenReservationEvents,
            }}
        >
            {children}
        </ReservationEventsContext.Provider>
    );
}

function waitUntilOpen(source: EventSource): Promise<void> {
    if (source.readyState === EventSource.OPEN) {
        return Promise.resolve();
    }

    if (source.readyState === EventSource.CLOSED) {
        return Promise.reject(
            new Error("La connessione alle notifiche della prenotazione è chiusa.")
        );
    }

    return new Promise((resolve, reject) => {
        const timeoutId = window.setTimeout(() => {
            cleanup();
            reject(new Error("Impossibile connettersi alle notifiche della prenotazione."));
        }, 5000);

        const handleOpen = () => {
            cleanup();
            resolve();
        };

        const cleanup = () => {
            window.clearTimeout(timeoutId);
            source.removeEventListener("open", handleOpen);
        };

        source.addEventListener("open", handleOpen);
    });
}

// The hook intentionally shares this module with its provider.
// eslint-disable-next-line react-refresh/only-export-components
export function useReservationEvents() {
    const ctx = useContext(ReservationEventsContext);

    if (!ctx) {
        throw new Error(
            "useReservationEvents must be used inside ReservationEventsProvider"
        );
    }

    return ctx;
}
