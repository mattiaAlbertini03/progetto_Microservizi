import { useAuth } from "../components/AuthContext";

export default function ProfilePage() {
    const { user, logout } = useAuth();

    if (!user) return null;

    const initial = user.name.trim().charAt(0).toUpperCase();

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
            </div>
        </main>
    );
}
