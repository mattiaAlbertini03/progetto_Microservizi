import { Link } from "react-router-dom";
import { useAuth } from "./AuthContext";

export default function Navbar() {
    const { user, openLogin, logout } = useAuth();

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
            <Link
                to="/"
                style={{
                    color: "white",
                    textDecoration: "none",
                    fontWeight: "bold",
                    fontSize: "1.1rem",
                }}
            >
                ACMEMobility
            </Link>

            <div
                style={{
                    display: "flex",
                    gap: "1rem",
                    alignItems: "center",
                }}
            >
                <Link
                    to="/"
                    style={{
                        color: "white",
                        textDecoration: "none",
                    }}
                >
                    Mappa
                </Link>

                {user ? (
                    <>
                        <Link
                            to="/reservations"
                            style={{
                                color: "white",
                                textDecoration: "none",
                            }}
                        >
                            Prenotazioni
                        </Link>

                        <Link
                            to="/profile"
                            style={{
                                color: "white",
                                textDecoration: "none",
                            }}
                        >
                            Profilo
                        </Link>

                        <span>{user.name}</span>

                        <button onClick={logout}>
                            Logout
                        </button>
                    </>
                ) : (
                    <button onClick={openLogin}>Login</button>
                )}
            </div>
        </nav>
    );
}