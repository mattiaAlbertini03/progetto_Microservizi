import { AuthProvider } from "./components/AuthContext";
import Navbar from "./components/Navbar";
import Map from "./components/Map";

export default function App() {
    return (
        <AuthProvider>
            <Navbar />
            <div style={{ paddingTop: "56px" }}>
                <Map />
            </div>
        </AuthProvider>
    );
}