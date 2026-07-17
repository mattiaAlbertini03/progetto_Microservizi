import {
    useCallback,
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";

import { loginUser } from "../api/acmeApi";
import { AUTH_EXPIRED_EVENT, getTokenExpirationMs } from "../authSession";
import type { LoginCredentials, User } from "../types/domain";

type AuthContextType = {
    user: User | null;
    isLoginOpen: boolean;
    authEntryMode: "login" | "signup";
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
    openLogin: () => void;
    openSignup: () => void;
    closeLogin: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(() => {
        const saved = localStorage.getItem("acme-user");

        if (!saved) {
            return null;
        }

        try {
            const storedUser = JSON.parse(saved) as User;
            const expiresAt = getTokenExpirationMs(storedUser.token);

            if (expiresAt !== null && expiresAt <= Date.now()) {
                localStorage.removeItem("acme-user");
                return null;
            }

            return storedUser;
        } catch {
            localStorage.removeItem("acme-user");
            return null;
        }
    });

    const [isLoginOpen, setIsLoginOpen] = useState(false);
    const [authEntryMode, setAuthEntryMode] = useState<"login" | "signup">("login");

    const openAuth = useCallback((mode: "login" | "signup") => {
        setAuthEntryMode(mode);
        setIsLoginOpen(true);
    }, []);

    async function login(credentials: LoginCredentials) {
        const loggedUser = await loginUser(credentials);

        localStorage.setItem("acme-user", JSON.stringify(loggedUser));
        setUser(loggedUser);
        setIsLoginOpen(false);
    }

    const logout = useCallback(() => {
        localStorage.removeItem("acme-user");
        setUser(null);
    }, []);

    useEffect(() => {
        const handleExpiredSession = () => {
            logout();
            openAuth("login");
        };

        window.addEventListener(AUTH_EXPIRED_EVENT, handleExpiredSession);
        return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpiredSession);
    }, [logout, openAuth]);

    useEffect(() => {
        if (!user) {
            return;
        }

        const expiresAt = getTokenExpirationMs(user.token);

        if (expiresAt === null) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            logout();
            openAuth("login");
        }, Math.max(0, expiresAt - Date.now()));

        return () => window.clearTimeout(timeoutId);
    }, [logout, openAuth, user]);

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoginOpen,
                authEntryMode,
                login,
                logout,
                openLogin: () => openAuth("login"),
                openSignup: () => openAuth("signup"),
                closeLogin: () => setIsLoginOpen(false),
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

// The hook intentionally shares this module with its provider.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
    const ctx = useContext(AuthContext);

    if (!ctx) {
        throw new Error("useAuth must be used inside AuthProvider");
    }

    return ctx;
}
