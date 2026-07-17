import { useEffect, useState } from "react";
import {
    changePassword,
    connectBankAccount,
    getBankAccount,
    rechargeBankAccount,
} from "../api/acmeApi";
import { useAuth } from "../components/AuthContext";
import type { BankAccount } from "../types/domain";

export default function ProfilePage() {
    const { user, logout } = useAuth();
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMessage, setPasswordMessage] = useState<string | null>(null);
    const [passwordError, setPasswordError] = useState<string | null>(null);
    const [savingPassword, setSavingPassword] = useState(false);
    const [bankAccount, setBankAccount] = useState<BankAccount | null>(null);
    const [bankLoading, setBankLoading] = useState(true);
    const [bankError, setBankError] = useState<string | null>(null);
    const [cardNumber, setCardNumber] = useState("");
    const [cardHolder, setCardHolder] = useState("");
    const [expiry, setExpiry] = useState("");
    const [cvv, setCvv] = useState("");
    const [rechargeAmount, setRechargeAmount] = useState("25");

    const initial = user?.name.trim().charAt(0).toUpperCase() ?? "";

    useEffect(() => {
        if (!user) return;
        let cancelled = false;
        setBankLoading(true);
        getBankAccount(user).then((account) => {
            if (!cancelled) setBankAccount(account);
        }).catch((error) => {
            console.error(error);
            if (!cancelled) setBankError("Servizio bancario non disponibile.");
        }).finally(() => {
            if (!cancelled) setBankLoading(false);
        });
        return () => { cancelled = true; };
    }, [user]);

    if (!user) return null;

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

    async function handleBankConnect(event: React.FormEvent) {
        event.preventDefault();
        setBankError(null);
        setBankLoading(true);
        try {
            const reservationRequestId = localStorage.getItem("acme-pending-bank-request") ?? undefined;
            const account = await connectBankAccount(user!, {
                cardNumber: cardNumber.replace(/\s/g, ""),
                cardHolder: cardHolder.trim(),
                expiry,
                cvv,
                reservationRequestId,
            });
            setBankAccount(account);
            localStorage.removeItem("acme-pending-bank-request");
            setCvv("");
        } catch (error) {
            console.error(error);
            setBankError("Carta demo non verificata o collegamento non riuscito.");
        } finally {
            setBankLoading(false);
        }
    }

    function handleCardNumberChange(value: string) {
        const digits = value.replace(/\D/g, "").slice(0, 16);
        setCardNumber(digits.replace(/(.{4})/g, "$1 ").trim());
    }

    function handleCvvChange(value: string) {
        setCvv(value.replace(/\D/g, "").slice(0, 3));
    }

    async function handleRecharge(event: React.FormEvent) {
        event.preventDefault();
        setBankError(null);
        setBankLoading(true);
        try {
            const reservationId = localStorage.getItem("acme-pending-recharge") ?? undefined;
            setBankAccount(await rechargeBankAccount(user!, Number(rechargeAmount), reservationId));
            localStorage.removeItem("acme-pending-recharge");
        } catch (error) {
            console.error(error);
            setBankError("Ricarica non riuscita.");
        } finally {
            setBankLoading(false);
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

                <section className="profile-details panel profile-bank">
                    <p className="eyebrow">Conto ACME Mobility</p>
                    <h2>{bankAccount?.connected ? "Saldo disponibile" : "Collega una carta demo"}</h2>
                    {bankLoading && <p className="muted-text">Aggiornamento conto…</p>}
                    {bankError && <div className="alert" role="alert">{bankError}</div>}

                    {bankAccount?.connected ? <>
                        <div className="balance-value">€ {(bankAccount.balance ?? 0).toFixed(2)}</div>
                        <p className="muted-text">Conto demo · {bankAccount.accountId}</p>
                        <form className="recharge-form" onSubmit={handleRecharge}>
                            <label className="field">Importo ricarica
                                <input type="number" min="1" max="500" step="0.01" required
                                    value={rechargeAmount} onChange={(event) => setRechargeAmount(event.target.value)} />
                            </label>
                            <button className="button" type="submit" disabled={bankLoading}>Ricarica saldo</button>
                        </form>
                    </> : <form onSubmit={handleBankConnect}>
                        <p className="demo-card-note">Solo simulazione: non inserire dati di una carta reale.</p>
                        <label className="field">Numero carta demo
                            <input inputMode="numeric" autoComplete="cc-number" required value={cardNumber}
                                minLength={19} maxLength={19}
                                onChange={(event) => handleCardNumberChange(event.target.value)}
                                placeholder="4242 4242 4242 4242" />
                        </label>
                        <label className="field">Intestatario
                            <input autoComplete="off" required value={cardHolder}
                                onChange={(event) => setCardHolder(event.target.value)} />
                        </label>
                        <div className="card-fields-row">
                            <label className="field">Scadenza
                                <input type="date" autoComplete="cc-exp" required value={expiry}
                                    min={new Date().toISOString().slice(0, 10)}
                                    onChange={(event) => setExpiry(event.target.value)} />
                            </label>
                            <label className="field">CVV demo
                                <input type="password" inputMode="numeric" autoComplete="cc-csc" required value={cvv}
                                    minLength={3} maxLength={3}
                                    onChange={(event) => handleCvvChange(event.target.value)} placeholder="123" />
                            </label>
                        </div>
                        <button className="button" type="submit" disabled={bankLoading}>Verifica e collega</button>
                    </form>}
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
