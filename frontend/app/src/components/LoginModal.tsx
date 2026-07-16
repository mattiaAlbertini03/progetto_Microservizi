import { useEffect, useState } from "react";
import { useAuth } from "./AuthContext";

export default function LoginModal() {
    const { isLoginOpen, closeLogin, login } = useAuth();
    const [username, setUsername] = useState("demo");
    const [password, setPassword] = useState("demo");
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!isLoginOpen) {
            return;
        }

        function closeOnEscape(event: KeyboardEvent) {
            if (event.key === "Escape") {
                closeLogin();
            }
        }

        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [closeLogin, isLoginOpen]);

    if (!isLoginOpen) {
        return null;
    }

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();

        try {
            setSubmitting(true);
            setError(null);
            await login({ username, password });
        } catch (err) {
            console.error(err);
            setError("Credenziali non valide. Controlla username e password.");
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <div
            className="modal-backdrop"
            role="presentation"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) closeLogin();
            }}
        >
            <form className="login-card" onSubmit={handleSubmit} role="dialog" aria-modal="true" aria-labelledby="login-title">
                <div className="login-heading">
                    <div>
                        <p className="eyebrow">Bentornato</p>
                        <h2 id="login-title">Accedi per partire</h2>
                        <p>Gestisci prenotazioni, corse e pagamenti.</p>
                    </div>
                    <button className="icon-button" type="button" onClick={closeLogin} aria-label="Chiudi">
                        ×
                    </button>
                </div>

                <label className="field">
                    Username
                    <input
                        autoFocus
                        autoComplete="username"
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                    />
                </label>

                <label className="field">
                    Password
                    <input
                        type="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                    />
                </label>

                {error && <div className="alert" role="alert">{error}</div>}

                <div className="modal-actions">
                    <button className="button button--secondary" type="button" onClick={closeLogin}>
                        Annulla
                    </button>
                    <button className="button" type="submit" disabled={submitting}>
                        {submitting ? "Accesso…" : "Accedi"}
                    </button>
                </div>
            </form>
        </div>
    );
}
