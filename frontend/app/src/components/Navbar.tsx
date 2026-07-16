import { Link, NavLink } from "react-router-dom";
import { useAuth } from "./AuthContext";

const navigation = [
    { to: "/", label: "Mappa", icon: "⌖", protected: false },
    { to: "/reservations", label: "Prenotazioni", icon: "▤", protected: true },
    { to: "/profile", label: "Profilo", icon: "●", protected: true },
];

function navClass({ isActive }: { isActive: boolean }) {
    return `nav-link${isActive ? " active" : ""}`;
}

function mobileNavClass({ isActive }: { isActive: boolean }) {
    return `mobile-nav-link${isActive ? " active" : ""}`;
}

export default function Navbar() {
    const { user, openLogin, logout } = useAuth();
    const initial = user?.name.trim().charAt(0).toUpperCase() || "A";

    return (
        <>
            <header className="topbar">
                <Link to="/" className="brand" aria-label="ACMEMobility home">
                    <span className="brand-mark">A</span>
                    <span className="brand-copy">
                        ACMEMobility
                        <small>Move electric</small>
                    </span>
                </Link>

                <nav className="desktop-nav" aria-label="Navigazione principale">
                    {navigation.filter((item) => user || !item.protected).map((item) => (
                        <NavLink key={item.to} to={item.to} className={navClass}>
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <div className="desktop-account">
                    {user ? (
                        <>
                            <span className="user-chip">
                                <span className="avatar">{initial}</span>
                                {user.name}
                            </span>
                            <button className="button button--ghost" onClick={logout}>
                                Esci
                            </button>
                        </>
                    ) : (
                        <button className="button button--accent" onClick={openLogin}>
                            Accedi
                        </button>
                    )}
                </div>

                <div className="mobile-account">
                    {user ? (
                        <Link to="/profile" className="avatar" aria-label="Apri profilo">
                            {initial}
                        </Link>
                    ) : (
                        <button className="button button--accent" onClick={openLogin}>
                            Accedi
                        </button>
                    )}
                </div>
            </header>

            <nav className={`mobile-nav${user ? "" : " mobile-nav--guest"}`} aria-label="Navigazione mobile">
                {navigation.filter((item) => user || !item.protected).map((item) => (
                    <NavLink key={item.to} to={item.to} className={mobileNavClass}>
                        <span className="mobile-nav-icon" aria-hidden="true">{item.icon}</span>
                        {item.label}
                    </NavLink>
                ))}
                {!user && (
                    <button className="mobile-nav-link" onClick={openLogin}>
                        <span className="mobile-nav-icon" aria-hidden="true">●</span>
                        Accedi
                    </button>
                )}
            </nav>
        </>
    );
}
