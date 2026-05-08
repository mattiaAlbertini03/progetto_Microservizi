import { createContext, useContext, useState } from "react";
import { ACME_API_URL } from '../App'

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

    async function login() {
        try {
            const response = await fetch(`${ACME_API_URL}/auth/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    username: "demo",
                    password: "demo",
                }),
            });

            if (!response.ok) {
                throw new Error("Login failed");
            }

            const data = await response.json();

            const user: User = {
                id: data.userId,
                name: data.username,
                email: data.email ?? "demo@acme.local",
            };

            localStorage.setItem("acme-user", JSON.stringify(user));
            setUser(user);
        } catch (error) {
            console.error("Login error:", error);
        }
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