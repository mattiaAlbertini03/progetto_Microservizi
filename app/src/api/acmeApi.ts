import { ACME_API_URL } from "../App";
import type {
    LoginCredentials,
    ReservationData,
    StationData,
    User,
    VehicleData,
} from "../types/domain";

type ApiErrorResponse = {
    status?: string;
    message?: string;
    requestId?: string;
    userId?: string;
};

async function request<T>(
    path: string,
    options?: RequestInit
): Promise<T> {
    const response = await fetch(`${ACME_API_URL}${path}`, options);

    if (!response.ok) {
        let errorMessage = `HTTP ${response.status}`;

        if (response.status === 401) {
            errorMessage = "Sessione non valida o token scaduto. Effettua nuovamente il login.";
        }

        try {
            const errorBody =
                (await response.json()) as ApiErrorResponse;

            if (errorBody.status && errorBody.message) {
                errorMessage =
                    `${errorBody.status}: ${errorBody.message}`;
            } else if (errorBody.message) {
                errorMessage = errorBody.message;
            }
        } catch {
            // Nessun JSON nella risposta.
        }

        throw new Error(errorMessage);
    }

    return response.json() as Promise<T>;
}

export async function loginUser(
    credentials: LoginCredentials
): Promise<User> {
    const data = await request<{
        userId: string;
        username: string;
        email?: string;
        token: string;
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
        token: data.token,
    };
}

export function getStations(): Promise<StationData[]> {
    return request<StationData[]>("/stations");
}

export function getVehiclesByStation(
    stationId: string
): Promise<VehicleData[]> {
    return request<VehicleData[]>(
        `/logistics/vehicles?stationId=${encodeURIComponent(
            stationId
        )}`
    );
}

export async function createReservation(
    user: User,
    vehicleId: string,
    stationId: string
): Promise<{ requestId: string }> {
    return request<{ requestId: string }>("/reservations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({
            userId: user.id,
            vehicleId,
            stationId,
        }),
    });
}

export function getReservations(
    user: User
): Promise<ReservationData[]> {
    return request<ReservationData[]>(
        `/reservations?userId=${encodeURIComponent(user.id)}`,
        {
            headers: {
                Authorization: `Bearer ${user.token}`,
            },
        }
    );
}

export async function sendReservationAction(
    user: User,
    reservationId: string,
    action: "pickup" | "cancel" | "return"
): Promise<void> {
    await request<unknown>(
        `/reservations/${encodeURIComponent(
            reservationId
        )}/${action}`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${user.token}`,
            },
        }
    );
}
