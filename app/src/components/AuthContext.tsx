import {
    createContext,
    useContext,
    useState,
    type ReactNode,
} from "react";

import { ACME_API_URL } from "../App";

type User = {
    id: string;
    name: string;
    email: string;
};

type LoginCredentials = {
    username: string;
    password: string;
};

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
        const response = await fetch(`${ACME_API_URL}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(credentials),
        });

        if (!response.ok) {
            throw new Error("Login failed");
        }

        const data = await response.json();

        const loggedUser: User = {
            id: data.userId,
            name: data.username,
            email: data.email ?? `${data.username}@acme.local`,
        };

        localStorage.setItem("acme-user", JSON.stringify(loggedUser));
        setUser(loggedUser);
        setIsLoginOpen(false);
    }

    function logout() {
        localStorage.removeItem("acme-user");
        setUser(null);
    }

    function openLogin() {
        setIsLoginOpen(true);
    }

    function closeLogin() {
        setIsLoginOpen(false);
    }

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoginOpen,
                login,
                logout,
                openLogin,
                closeLogin,
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