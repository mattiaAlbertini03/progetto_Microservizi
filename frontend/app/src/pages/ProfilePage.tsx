import { useState } from "react";
import { changePassword } from "../api/acmeApi";
import { useAuth } from "../components/AuthContext";

export default function ProfilePage() {
    const { user, logout } = useAuth();
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMessage, setPasswordMessage] = useState<string | null>(null);
    const [passwordError, setPasswordError] = useState<string | null>(null);
    const [savingPassword, setSavingPassword] = useState(false);

    if (!user) return null;

    const initial = user.name.trim().charAt(0).toUpperCase();

    async function handlePasswordChange(event: React.FormEvent) {
        event.preventDefault();
        setPasswordError(null);
        setPasswordMessage(null);
        if (newPassword !== confirmPassword) {
            setPasswordError("Le nuove password non coincidono.");
            return;
        }
        try {
            setSavingPassword(true);
            await changePassword(user!, currentPassword, newPassword);
            setCurrentPassword("");
            setNewPassword("");
            setConfirmPassword("");
            setPasswordMessage("Password aggiornata.");
        } catch (error) {
            console.error(error);
            setPasswordError("Password attuale non valida o aggiornamento non riuscito.");
        } finally {
            setSavingPassword(false);
        }
    }

    return (
        <main className="page">
            <header className="page-header">
                <div>
                    <p className="eyebrow">Account personale</p>
                    <h1 className="page-title">Il tuo profilo</h1>
                    <p className="page-subtitle">I dati usati per prenotazioni, corse e notifiche ACMEMobility.</p>
                </div>
            </header>

            <div className="profile-grid">
                <section className="profile-hero panel">
                    <div className="profile-avatar">{initial}</div>
                    <h2>{user.name}</h2>
                    <p>{user.email}</p>
                    <button className="button button--danger" onClick={logout}>Esci dall'account</button>
                </section>

                <section className="profile-details panel">
                    <p className="eyebrow">Dettagli account</p>
                    <div className="detail-row">
                        <span className="detail-label">Nome utente</span>
                        <span className="detail-value">{user.name}</span>
                    </div>
                    <div className="detail-row">
                        <span className="detail-label">Email</span>
                        <span className="detail-value">{user.email}</span>
                    </div>
                    <div className="detail-row">
                        <span className="detail-label">ID utente</span>
                        <span className="detail-value">{user.id}</span>
                    </div>
                    <div className="detail-row">
                        <span className="detail-label">Stato</span>
                        <span className="detail-value">● Account attivo</span>
                    </div>
                </section>

                <form className="profile-details panel profile-password" onSubmit={handlePasswordChange}>
                    <p className="eyebrow">Sicurezza</p>
                    <h2>Cambia password</h2>
                    <label className="field">Password attuale
                        <input type="password" autoComplete="current-password" required value={currentPassword}
                            onChange={(event) => setCurrentPassword(event.target.value)} />
                    </label>
                    <label className="field">Nuova password
                        <input type="password" autoComplete="new-password" minLength={8} required value={newPassword}
                            onChange={(event) => setNewPassword(event.target.value)} />
                    </label>
                    <label className="field">Conferma nuova password
                        <input type="password" autoComplete="new-password" minLength={8} required value={confirmPassword}
                            onChange={(event) => setConfirmPassword(event.target.value)} />
                    </label>
                    {passwordError && <div className="alert" role="alert">{passwordError}</div>}
                    {passwordMessage && <div className="success-message" role="status">{passwordMessage}</div>}
                    <button className="button" type="submit" disabled={savingPassword}>
                        {savingPassword ? "Salvataggio…" : "Aggiorna password"}
                    </button>
                </form>
            </div>
        </main>
    );
}
