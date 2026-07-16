import { useEffect, useState } from "react";
import Vehicle from "./Vehicle";
import type { StationData, VehicleData } from "../types/domain";

type StationProps = {
    station: StationData;
    vehicles?: VehicleData[];
    loading?: boolean;
    onReserve?: (
        station: StationData,
        vehicle: VehicleData,
        scheduledStartEpochMs: number
    ) => void | Promise<void>;
    onQrPickup?: (station: StationData, vehicle: VehicleData) => void | Promise<void>;
};

const SLOT_DURATION_MS = 5 * 60_000;
const RESERVATION_WINDOW_MS = 30 * 60_000;

function roundUpToSlot(epochMs: number) {
    return Math.ceil(epochMs / SLOT_DURATION_MS) * SLOT_DURATION_MS;
}

function buildTimeSlots(nowEpochMs: number) {
    const firstSlot = roundUpToSlot(nowEpochMs);
    const lastAllowedStart = nowEpochMs + RESERVATION_WINDOW_MS;
    const slots: Array<{ value: number; label: string }> = [];

    for (
        let slot = firstSlot;
        slot <= lastAllowedStart;
        slot += SLOT_DURATION_MS
    ) {
        const minutesFromNow = Math.max(
            0,
            Math.ceil((slot - nowEpochMs) / 60_000)
        );
        const time = new Date(slot).toLocaleTimeString("it-IT", {
            hour: "2-digit",
            minute: "2-digit",
        });

        slots.push({
            value: slot,
            label: `${time} (tra ${minutesFromNow} min)`,
        });
    }

    return slots;
}

export default function Station({
    station,
    vehicles,
    loading = false,
    onReserve,
    onQrPickup,
}: StationProps) {
    const [now, setNow] = useState(Date.now);
    const [scheduledStart, setScheduledStart] = useState(
        roundUpToSlot(now + 10 * 60_000)
    );
    const timeSlots = buildTimeSlots(now);
    const selectedSlot = timeSlots.some(
        (slot) => slot.value === scheduledStart
    )
        ? scheduledStart
        : timeSlots[0].value;

    useEffect(() => {
        const intervalId = window.setInterval(() => {
            setNow(Date.now());
        }, 30_000);

        return () => window.clearInterval(intervalId);
    }, []);

    return (
        <div>
            <strong>{station.name}</strong>

            <div style={{ fontSize: "0.85rem", color: "#555", marginTop: "0.25rem" }}>
                Capacità: {station.capacity}
            </div>

            <hr />

            <strong>Veicoli</strong>

            {onReserve && (
                <label style={{ display: "block", margin: "0.5rem 0" }}>
                    Fascia oraria (solo nei prossimi 30 minuti)
                    <select
                        value={selectedSlot}
                        onChange={(event) => setScheduledStart(Number(event.target.value))}
                        style={{ display: "block", width: "100%", marginTop: "0.25rem" }}
                    >
                        {timeSlots.map((slot) => (
                            <option key={slot.value} value={slot.value}>
                                {slot.label}
                            </option>
                        ))}
                    </select>
                    <small style={{ color: "#555" }}>
                        Intervalli di 5 minuti; le opzioni scadute scompaiono automaticamente.
                    </small>
                </label>
            )}

            {loading && <p>Caricamento veicoli...</p>}

            {!loading && vehicles?.length === 0 && (
                <p>Nessun veicolo disponibile.</p>
            )}

            {!loading &&
                vehicles?.map((vehicle) => (
                    <Vehicle
                        key={vehicle.id}
                        vehicle={vehicle}
                        onReserve={
                            onReserve
                                ? () => onReserve(
                                    station,
                                    vehicle,
                                    selectedSlot
                                )
                                : undefined
                        }
                        onQrPickup={
                            onQrPickup
                                ? () => onQrPickup(station, vehicle)
                                : undefined
                        }
                    />
                ))}
        </div>
    );
}
