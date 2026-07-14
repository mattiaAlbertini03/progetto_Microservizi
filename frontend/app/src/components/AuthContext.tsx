import {
    createContext,
    useContext,
    useState,
    type ReactNode,
} from "react";

import { loginUser } from "../api/acmeApi";
import type { LoginCredentials, User } from "../types/domain";

type AuthContextType = {
    user: User | null;
    isLoginOpen: boolean;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
    openLogin: () => void;
    closeLogin: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(() => {
        const saved = localStorage.getItem("acme-user");
        return saved ? JSON.parse(saved) : null;
    });

    const [isLoginOpen, setIsLoginOpen] = useState(false);

    async function login(credentials: LoginCredentials) {
        const loggedUser = await loginUser(credentials);

        localStorage.setItem("acme-user", JSON.stringify(loggedUser));
        setUser(loggedUser);
        setIsLoginOpen(false);
    }

    function logout() {
        localStorage.removeItem("acme-user");
        setUser(null);
    }

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoginOpen,
                login,
                logout,
                openLogin: () => setIsLoginOpen(true),
                closeLogin: () => setIsLoginOpen(false),
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);

    if (!ctx) {
        throw new Error("useAuth must be used inside AuthProvider");
    }

    return ctx;
}