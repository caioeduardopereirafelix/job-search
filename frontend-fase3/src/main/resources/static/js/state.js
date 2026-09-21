const STORAGE_KEY = "job-search-session";
export function saveSession(session) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}
export function loadSession() {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    }
    catch {
        return null;
    }
}
export function clearSession() {
    sessionStorage.removeItem(STORAGE_KEY);
}
