import { ACME_API_URL } from "../App";
import type {
    LoginCredentials,
    ReservationData,
    StationData,
    User,
    VehicleData,
} from "../types/domain";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${ACME_API_URL}${path}`, options);

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

export async function loginUser(credentials: LoginCredentials): Promise<User> {
    const data = await request<{
        userId: string;
        username: string;
        email?: string;
    }>("/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(credentials),
    });

    return {
        id: data.userId,
        name: data.username,
        email: data.email ?? `${data.username}@acme.local`,
    };
}

export function getStations(): Promise<StationData[]> {
    return request<StationData[]>("/stations");
}

export function getVehiclesByStation(stationId: string): Promise<VehicleData[]> {
    return request<VehicleData[]>(
        `/logistics/vehicles?stationId=${encodeURIComponent(stationId)}`
    );
}

export async function createReservation(
    userId: string,
    vehicleId: string,
    stationId: string
): Promise<{ requestId: string }> {
    return request<{ requestId: string }>("/reservations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${userId}`,
        },
        body: JSON.stringify({
            userId,
            vehicleId,
            stationId,
        }),
    });
}

export function getReservations(userId: string): Promise<ReservationData[]> {
    return request<ReservationData[]>(`/reservations?userId=${userId}`, {
        headers: {
            Authorization: `Bearer ${userId}`,
        },
    });
}

export async function sendReservationAction(
    userId: string,
    reservationId: string,
    action: "pickup" | "cancel" | "return"
): Promise<void> {
    const response = await fetch(
        `${ACME_API_URL}/reservations/${reservationId}/${action}`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${userId}`,
            },
        }
    );

    if (!response.ok) {
        if (response.status === 403) {
            throw new Error("FORBIDDEN");
        }

        if (response.status === 409) {
            throw new Error("CONFLICT");
        }

        throw new Error(`HTTP ${response.status}`);
    }
}