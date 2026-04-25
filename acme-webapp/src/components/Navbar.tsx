import { useAuth } from "./AuthContext";

export default function Navbar() {
    const { user, login, logout } = useAuth();

    return (
        <nav
            style={{
                height: "56px",
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "0 1.5rem",
                background: "#111827",
                color: "white",
                position: "fixed",
                top: 0,
                left: 0,
                right: 0,
                zIndex: 1000,
            }}
        >
            <strong>ACMEMobility</strong>

            <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
                <span>Mappa</span>
                <span>Prenotazioni</span>
                <span>Noleggi</span>
                <span>Profilo</span>

                {user ? (
                    <>
                        <span>{user.name}</span>
                        <button onClick={logout}>Logout</button>
                    </>
                ) : (
                    <button onClick={login}>Login demo</button>
                )}
            </div>
        </nav>
    );
}