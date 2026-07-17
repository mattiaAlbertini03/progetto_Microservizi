export type User = {
    id: string;
    name: string;
    email: string;
    token: string;
};

export type LoginCredentials = {
    username: string;
    password: string;
};

export type SignupData = {
    username: string;
    email: string;
    password: string;
};

export type StationData = {
    id: string;
    name: string;
    lat: number;
    lon: number;
};

export type VehicleData = {
    id: string;
    name: string;
    type: "CAR" | "SCOOTER" | "KICKSCOOTER" | string;
    lat?: number | null;
    lon?: number | null;
    status: string;
    batteryPct: number;
    qrCode?: string;
};

export type ReservationData = {
    reservationId: string;
    userId: string;
    vehicleId: string;
    stationId: string;
    status: string;
    createdAtEpochMs: number;
    scheduledStartEpochMs: number;
    pickupDeadlineEpochMs: number;
    totalKilometers?: number | null;
    paymentAmount?: number | null;
    completedAtEpochMs?: number | null;
};
