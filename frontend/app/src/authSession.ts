export const AUTH_EXPIRED_EVENT = "acme-auth-expired";

export function notifyAuthExpired() {
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
}

export function getTokenExpirationMs(token: string): number | null {
    try {
        const payloadPart = token.split(".")[1];

        if (!payloadPart) {
            return null;
        }

        const normalized = payloadPart
            .replace(/-/g, "+")
            .replace(/_/g, "/")
            .padEnd(Math.ceil(payloadPart.length / 4) * 4, "=");
        const payload = JSON.parse(window.atob(normalized)) as { exp?: unknown };

        return typeof payload.exp === "number" ? payload.exp * 1000 : null;
    } catch {
        return null;
    }
}
