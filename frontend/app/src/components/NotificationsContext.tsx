import { createContext, useCallback, useContext, useState, type ReactNode } from "react";

type NotificationTone = "info" | "success" | "error";
type Notification = { id: number; message: string; tone: NotificationTone };

const NotificationsContext = createContext<((message: string, tone?: NotificationTone) => void) | null>(null);

export function NotificationsProvider({ children }: { children: ReactNode }) {
    const [notifications, setNotifications] = useState<Notification[]>([]);

    const notify = useCallback((message: string, tone: NotificationTone = "info") => {
        const id = Date.now() + Math.random();
        setNotifications((current) => [...current, { id, message, tone }]);
        window.setTimeout(() => {
            setNotifications((current) => current.filter((item) => item.id !== id));
        }, 4500);
    }, []);

    return (
        <NotificationsContext.Provider value={notify}>
            {children}
            <div className="toast-region" aria-live="polite" aria-label="Notifiche">
                {notifications.map((notification) => (
                    <div key={notification.id} className={`toast toast--${notification.tone}`}>
                        <span>{notification.tone === "success" ? "✓" : notification.tone === "error" ? "!" : "i"}</span>
                        <p>{notification.message}</p>
                        <button onClick={() => setNotifications((current) => current.filter((item) => item.id !== notification.id))} aria-label="Chiudi notifica">×</button>
                    </div>
                ))}
            </div>
        </NotificationsContext.Provider>
    );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
    const notify = useContext(NotificationsContext);
    if (!notify) throw new Error("useNotifications must be used inside NotificationsProvider");
    return notify;
}
