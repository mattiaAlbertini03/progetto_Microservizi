import {
    createContext,
    useContext,
    useRef,
    type ReactNode,
} from "react";

import { ACME_API_URL } from "../App";

type ReservationEventsContextType = {
    listenReservationRequest: (requestId: string, stationId?: string) => void;
    listenReservationEvents: (reservationId: string) => EventSource;
};

const ReservationEventsContext =
    createContext<ReservationEventsContextType | null>(null);

export function ReservationEventsProvider({ children }: { children: ReactNode }) {
    const sources = useRef<Record<string, EventSource>>({});

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

    function listenReservationEvents(reservationId: string) {
        const key = `return-${reservationId}`;

        if (sources.current[key]) {
            return sources.current[key];
        }

        const eventSource = new EventSource(
            `${ACME_API_URL}/reservations/${reservationId}/events`
        );

        sources.current[key] = eventSource;

        eventSource.addEventListener("reservation-update", (event) => {
            const data = JSON.parse((event as MessageEvent).data);

            switch (data.status) {
                case "VEHICLE_NOT_AT_DESTINATION":
                    alert("Il veicolo non è ancora arrivato alla stazione.");
                    break;

                case "RETURN_CONFIRMED":
                    alert("Veicolo riconsegnato correttamente.");

                    window.dispatchEvent(
                        new CustomEvent("reservations-changed")
                    );

                    eventSource.close();
                    delete sources.current[key];
                    break;

                case "RETURN_FAILED":
                    alert("Errore durante la riconsegna.");

                    eventSource.close();
                    delete sources.current[key];
                    break;
            }
        });

        eventSource.onerror = () => {
            console.error("Errore SSE return");
        };

        return eventSource;
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

export function useReservationEvents() {
    const ctx = useContext(ReservationEventsContext);

    if (!ctx) {
        throw new Error(
            "useReservationEvents must be used inside ReservationEventsProvider"
        );
    }

    return ctx;
}