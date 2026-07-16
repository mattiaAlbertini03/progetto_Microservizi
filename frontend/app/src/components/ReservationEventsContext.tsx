import {
    createContext,
    useContext,
    useEffect,
    useRef,
    type ReactNode,
} from "react";

import { ACME_API_URL } from "../App";

type ReservationEventsContextType = {
    listenReservationRequest: (requestId: string, stationId?: string) => void;
    listenReservationEvents: (reservationId: string) => Promise<EventSource>;
};

const ReservationEventsContext =
    createContext<ReservationEventsContextType | null>(null);

export function ReservationEventsProvider({ children }: { children: ReactNode }) {
    const sources = useRef<Record<string, EventSource>>({});

    useEffect(() => {
        return () => {
            Object.values(sources.current).forEach((source) => source.close());
            sources.current = {};
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
                case "RESERVED":
                    alert("Prenotazione confermata");

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
                    alert(data.message ?? "Prenotazione fallita");

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
                    alert(
                        data.message ??
                            "Il deposito è stato trattenuto con successo."
                    );
                    break;

                case "RETURN_REQUEST_ACCEPTED":
                    alert(data.message ?? "Richiesta di riconsegna accettata.");
                    break;

                case "VEHICLE_NOT_AT_DESTINATION":
                    alert(data.message);
                    break;

                case "LOW_BATTERY_PENALTY_APPLIED":
                    alert(
                        data.message ??
                            "Batteria sotto il 15%: applicata una penale del 10%."
                    );
                    break;

                case "DEPOSIT_RELEASED":
                    alert(
                        data.message ??
                            "Il deposito è stato rilasciato con successo."
                    );
                    break;

                case "RETURN_CONFIRMED":
                    alert(data.message);

                    window.dispatchEvent(
                        new CustomEvent("reservations-changed")
                    );

                    eventSource.close();
                    delete sources.current[key];
                    break;

                case "RETURN_FAILED":
                    alert(data.message);

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
