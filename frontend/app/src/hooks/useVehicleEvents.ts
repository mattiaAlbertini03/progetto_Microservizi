import { useEffect } from "react";
import { ACME_API_URL } from "../App";

export function useVehicleEvents(
    stationId: string | null | undefined,
    onVehicleUpdate: () => void
) {
    useEffect(() => {
        if (!stationId) {
            return;
        }

        const source = new EventSource(
            `${ACME_API_URL}/stations/${stationId}/vehicles/events`
        );

        source.addEventListener("vehicle-update", () => {
            onVehicleUpdate();
        });

        source.onerror = () => {
            // EventSource reconnects automatically after transient failures.
        };

        return () => {
            source.close();
        };
    }, [stationId, onVehicleUpdate]);
}
