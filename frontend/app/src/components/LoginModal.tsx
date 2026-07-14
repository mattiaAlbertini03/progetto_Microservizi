import { useState } from "react";

import { useAuth } from "./AuthContext";

export default function LoginModal() {
    const { isLoginOpen, closeLogin, login } = useAuth();

    const [username, setUsername] = useState("demo");
    const [password, setPassword] = useState("demo");
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    if (!isLoginOpen) {
        return null;
    }

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();

        try {
            setSubmitting(true);
            setError(null);

            await login({
                username,
                password,
            });
        } catch (err) {
            console.error(err);
            setError("Credenziali non valide");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div
            style={{
                position: "fixed",
                inset: 0,
                background: "rgba(0,0,0,0.45)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                zIndex: 2000,
            }}
        >
            <form
                onSubmit={handleSubmit}
                style={{
                    width: "360px",
                    padding: "1.5rem",
                    borderRadius: "12px",
                    background: "white",
                    color: "#111827",
                    boxShadow: "0 10px 30px rgba(0,0,0,0.25)",
                }}
            >
                <h2 style={{ marginTop: 0 }}>Login</h2>

                <label>
                    Username
                    <input
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        style={{
                            width: "100%",
                            marginTop: "0.25rem",
                            marginBottom: "1rem",
                            padding: "0.5rem",
                        }}
                    />
                </label>

                <label>
                    Password
                    <input
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        style={{
                            width: "100%",
                            marginTop: "0.25rem",
                            marginBottom: "1rem",
                            padding: "0.5rem",
                        }}
                    />
                </label>

                {error && (
                    <p style={{ color: "red", marginTop: 0 }}>
                        {error}
                    </p>
                )}

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "0.5rem",
                    }}
                >
                    <button type="button" onClick={closeLogin}>
                        Annulla
                    </button>

                    <button type="submit" disabled={submitting}>
                        {submitting ? "Accesso..." : "Login"}
                    </button>
                </div>
            </form>
        </div>
    );
}