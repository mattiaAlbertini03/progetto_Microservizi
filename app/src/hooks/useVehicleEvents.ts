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

        console.log("Vehicle SSE opening", stationId);

        const source = new EventSource(
            `${ACME_API_URL}/stations/${stationId}/vehicles/events`
        );

        source.onopen = () => {
            console.log("Vehicle SSE opened", stationId);
        };

        source.addEventListener("vehicle-update", (event) => {
            console.log("Vehicle SSE received", stationId, event);
            onVehicleUpdate();
        });

        source.onerror = (error) => {
            console.error("Vehicle SSE error", stationId, error);
        };

        return () => {
            console.log("Vehicle SSE closing", stationId);
            source.close();
        };
    }, [stationId, onVehicleUpdate]);
}