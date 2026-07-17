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
        <div className="station-content">
            <header className="station-title">
                <div className="station-symbol">⚡</div>
                <div>
                    <p className="eyebrow">Stazione ACMEMobility</p>
                    <h2>{station.name}</h2>
                    <p>{vehicles?.filter((vehicle) => vehicle.status === "AVAILABLE" || (vehicle.status === "CHARGING" && vehicle.batteryPct >= 80)).length ?? 0} mezzi disponibili</p>
                </div>
            </header>

            {onReserve && (
                <label className="time-slot-card">
                    <span>
                        <strong>Quando vuoi partire?</strong>
                        <small>Seleziona un orario entro 30 minuti</small>
                    </span>
                    <select
                        className="time-slot-select"
                        value={selectedSlot}
                        onChange={(event) => setScheduledStart(Number(event.target.value))}
                    >
                        {timeSlots.map((slot) => (
                            <option key={slot.value} value={slot.value}>
                                {slot.label}
                            </option>
                        ))}
                    </select>
                </label>
            )}

            <div className="vehicle-list-heading">
                <strong>Mezzi disponibili</strong>
                <span>Aggiornamento live</span>
            </div>

            {loading && (
                <div className="vehicle-loading">
                    <span className="spinner" /> Caricamento mezzi…
                </div>
            )}

            {!loading && vehicles?.length === 0 && (
                <div className="vehicle-empty">Nessun veicolo disponibile in questa stazione.</div>
            )}

            {!loading && (
                <div className="vehicle-list">
                    {vehicles?.map((vehicle) => (
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
            )}
        </div>
    );
}
