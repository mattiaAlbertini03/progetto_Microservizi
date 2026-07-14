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

export type StationData = {
    id: string;
    name: string;
    lat: number;
    lon: number;
    capacity: number;
};

export type VehicleData = {
    id: string;
    type: "CAR" | "SCOOTER" | "KICKSCOOTER" | string;
    lat?: number | null;
    lon?: number | null;
    status: string;
    batteryPct: number;
    qrCode?: string;
};

export type ReservationData = {
    id: string;
    userId: string;
    vehicleId: string;
    stationId: string;
    status: string;
};