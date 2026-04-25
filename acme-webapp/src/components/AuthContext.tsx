import { createContext, useContext, useState } from "react";

type User = {
    id: string;
    name: string;
    email: string;
};

type AuthContextType = {
    user: User | null;
    login: () => void;
    logout: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(() => {
        const saved = localStorage.getItem("acme-user");
        return saved ? JSON.parse(saved) : null;
    });

    function login() {
        const demoUser = {
            id: "user-1",
            name: "Mario Rossi",
            email: "mario.rossi@example.com",
        };

        localStorage.setItem("acme-user", JSON.stringify(demoUser));
        setUser(demoUser);
    }

    function logout() {
        localStorage.removeItem("acme-user");
        setUser(null);
    }

    return (
        <AuthContext.Provider value={{ user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
    return ctx;
}