import { useAuth } from "../components/AuthContext";

export default function ProfilePage() {
    const { user, logout } = useAuth();

    if (!user) {
        return null;
    }

    return (
        <main style={{ padding: "1.5rem" }}>
            <h1>Profilo</h1>

            <div
                style={{
                    maxWidth: "480px",
                    padding: "1rem",
                    border: "1px solid #ddd",
                    borderRadius: "8px",
                    background: "white",
                }}
            >
                <h2>{user.name}</h2>

                <p>
                    <strong>ID utente:</strong> {user.id}
                </p>

                <p>
                    <strong>Stato:</strong> autenticato
                </p>

                <button onClick={logout}>
                    Logout
                </button>
            </div>
        </main>
    );
}