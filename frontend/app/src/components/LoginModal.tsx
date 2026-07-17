import { useEffect, useState } from "react";
import { requestPasswordReset, signupUser } from "../api/acmeApi";
import { useAuth } from "./AuthContext";

type AuthMode = "login" | "signup" | "forgot";

export default function LoginModal() {
    const { isLoginOpen, closeLogin, login } = useAuth();
    const [mode, setMode] = useState<AuthMode>("login");
    const [identifier, setIdentifier] = useState("demo");
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("demo");
    const [error, setError] = useState<string | null>(null);
    const [message, setMessage] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!isLoginOpen) return;
        function closeOnEscape(event: KeyboardEvent) {
            if (event.key === "Escape") closeLogin();
        }
        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [closeLogin, isLoginOpen]);

    if (!isLoginOpen) return null;

    function selectMode(nextMode: AuthMode) {
        setMode(nextMode);
        setError(null);
        setMessage(null);
    }

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        setSubmitting(true);
        setError(null);
        setMessage(null);
        try {
            if (mode === "login") {
                await login({ username: identifier, password });
            } else if (mode === "signup") {
                await signupUser({ username, email, password });
                setIdentifier(username);
                setPassword("");
                setMode("login");
                setMessage("Account creato. Ora puoi accedere.");
            } else {
                await requestPasswordReset(email);
                setMessage("Se l'email è registrata, riceverai una password temporanea.");
            }
        } catch (err) {
            console.error(err);
            setError(mode === "login"
                ? "Credenziali non valide. Usa username oppure email."
                : mode === "signup"
                    ? "Registrazione non riuscita. Username o email potrebbero essere già usati."
                    : "Invio non disponibile. Riprova più tardi.");
        } finally {
            setSubmitting(false);
        }
    }

    const title = mode === "login" ? "Accedi per partire"
        : mode === "signup" ? "Crea il tuo account" : "Recupera password";

    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeLogin();
        }}>
            <form className="login-card" onSubmit={handleSubmit} role="dialog" aria-modal="true" aria-labelledby="login-title">
                <div className="login-heading">
                    <div>
                        <p className="eyebrow">ACME Mobility</p>
                        <h2 id="login-title">{title}</h2>
                        <p>{mode === "forgot" ? "Inserisci l'email associata all'account." : "Gestisci prenotazioni, corse e pagamenti."}</p>
                    </div>
                    <button className="icon-button" type="button" onClick={closeLogin} aria-label="Chiudi">×</button>
                </div>

                {mode === "login" && <>
                    <label className="field">Username o email
                        <input autoFocus autoComplete="username" required value={identifier}
                            onChange={(event) => setIdentifier(event.target.value)} />
                    </label>
                    <label className="field">Password
                        <input type="password" autoComplete="current-password" required value={password}
                            onChange={(event) => setPassword(event.target.value)} />
                    </label>
                </>}

                {mode === "signup" && <>
                    <label className="field">Username
                        <input autoFocus autoComplete="username" required value={username}
                            onChange={(event) => setUsername(event.target.value)} />
                    </label>
                    <label className="field">Email
                        <input type="email" autoComplete="email" required value={email}
                            onChange={(event) => setEmail(event.target.value)} />
                    </label>
                    <label className="field">Password
                        <input type="password" autoComplete="new-password" minLength={8} required value={password}
                            onChange={(event) => setPassword(event.target.value)} />
                    </label>
                </>}

                {mode === "forgot" && <label className="field">Email
                    <input autoFocus type="email" autoComplete="email" required value={email}
                        onChange={(event) => setEmail(event.target.value)} />
                </label>}

                {error && <div className="alert" role="alert">{error}</div>}
                {message && <div className="success-message" role="status">{message}</div>}

                <div className="auth-switches">
                    {mode !== "login" && <button type="button" className="text-button" onClick={() => selectMode("login")}>Torna al login</button>}
                    {mode === "login" && <>
                        <button type="button" className="text-button" onClick={() => selectMode("signup")}>Registrati</button>
                        <button type="button" className="text-button" onClick={() => selectMode("forgot")}>Password dimenticata?</button>
                    </>}
                </div>

                <div className="modal-actions">
                    <button className="button button--secondary" type="button" onClick={closeLogin}>Annulla</button>
                    <button className="button" type="submit" disabled={submitting}>
                        {submitting ? "Invio…" : mode === "login" ? "Accedi" : mode === "signup" ? "Registrati" : "Invia password"}
                    </button>
                </div>
            </form>
        </div>
    );
}
