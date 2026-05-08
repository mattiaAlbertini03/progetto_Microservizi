import { AuthProvider } from "./components/AuthContext";
import Navbar from "./components/Navbar";
import Map from "./components/Map";

export const ACME_API_URL = "http://192.168.49.2:30080";

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