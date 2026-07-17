export async function requestBrowserNotificationPermission(): Promise<NotificationPermission | "unsupported"> {
    if (!("Notification" in window)) return "unsupported";
    if (Notification.permission === "default") return Notification.requestPermission();
    return Notification.permission;
}

export function showBrowserNotification(title: string, body: string): boolean {
    if (!("Notification" in window) || Notification.permission !== "granted") return false;
    const notification = new Notification(title, { body, icon: "/favicon.svg", tag: "vehicle-parked" });
    notification.onclick = () => { window.focus(); notification.close(); };
    return true;
}
