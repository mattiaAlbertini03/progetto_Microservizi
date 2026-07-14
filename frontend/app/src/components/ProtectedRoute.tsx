import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { JSX } from "react";

type ProtectedRouteProps = {
    children: JSX.Element;
};

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/" replace />;
    }

    return children;
}