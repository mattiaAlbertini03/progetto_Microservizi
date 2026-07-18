import { ACME_API_URL } from "../App";
import { notifyAuthExpired } from "../authSession";
import type {
    LoginCredentials,
    ReservationData,
    StationData,
    User,
    SignupData,
    BankAccount,
    FakeCardData,
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
    options?: RequestInit,
    notifyOnUnauthorized = true
): Promise<T> {
    const response = await fetch(`${ACME_API_URL}${path}`, options);

    if (!response.ok) {
        let errorMessage = `HTTP ${response.status}`;

        if (response.status === 401 && notifyOnUnauthorized) {
            notifyAuthExpired();
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
    }, false);

    return {
        id: data.userId,
        name: data.username,
        email: data.email ?? `${data.username}@acme.local`,
        token: data.token,
    };
}

export function signupUser(data: SignupData): Promise<{
    userId: string;
    username: string;
    email: string;
}> {
    return request("/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }, false);
}

export async function requestPasswordReset(email: string): Promise<void> {
    const response = await fetch(`${ACME_API_URL}/auth/password/forgot`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
}

export async function changePassword(
    user: User,
    currentPassword: string,
    newPassword: string
): Promise<void> {
    const response = await fetch(`${ACME_API_URL}/auth/password/change`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({ currentPassword, newPassword }),
    });
    if (!response.ok) {
        let message = `HTTP ${response.status}`;
        try {
            const body = await response.json() as ApiErrorResponse;
            message = body.message ?? message;
        } catch {
            // Empty or non-JSON error response.
        }
        throw new Error(message);
    }
}

export function getBankAccount(user: User): Promise<BankAccount> {
    return request<BankAccount>("/bank/account", {
        headers: { Authorization: `Bearer ${user.token}` },
    });
}

export function registerBankAccount(user: User, card: FakeCardData): Promise<BankAccount> {
    return request<BankAccount>("/bank/account/register", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify(card),
    });
}

export function authorizeBankAccountForReservation(user: User, reservationRequestId: string): Promise<BankAccount> {
    return request<BankAccount>("/bank/account/authorize-reservation", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({ reservationRequestId }),
    });
}

export function rechargeBankAccount(
    user: User,
    amount: number,
    reservationId?: string
): Promise<BankAccount> {
    return request<BankAccount>("/bank/account/recharge", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({ amount, reservationId }),
    });
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

export function getVehicle(vehicleId: string): Promise<VehicleData> {
    return request<VehicleData>(`/logistics/vehicles/${encodeURIComponent(vehicleId)}`);
}

export async function createReservation(
    user: User,
    vehicleId: string,
    stationId: string,
    scheduledStartEpochMs: number
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
            scheduledStartEpochMs,
        }),
    });
}

export async function createQrReservation(
    user: User,
    vehicleId: string,
    stationId: string,
    qrCode: string
): Promise<{ requestId: string }> {
    return request<{ requestId: string }>("/reservations/qr-pickup", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${user.token}`,
        },
        body: JSON.stringify({
            userId: user.id,
            vehicleId,
            stationId,
            qrCode,
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
