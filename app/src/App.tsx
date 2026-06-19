import { BrowserRouter, Routes, Route } from "react-router-dom";

import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";

import MapPage from "./pages/MapPage";
import ReservationsPage from "./pages/ReservationsPage";
import ProfilePage from "./pages/ProfilePage";
import LoginModal from "./components/LoginModal";

export const ACME_API_URL = "http://192.168.49.2:30080";

export default function App() {
    return (
        <BrowserRouter>
            <Navbar />
            <LoginModal />

            <div style={{ paddingTop: "56px" }}>
                <Routes>
                    <Route path="/" element={<MapPage />} />
                    <Route
                        path="/reservations"
                        element={
                            <ProtectedRoute>
                                <ReservationsPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/profile"
                        element={
                            <ProtectedRoute>
                                <ProfilePage />
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </div>
        </BrowserRouter>
    );
}