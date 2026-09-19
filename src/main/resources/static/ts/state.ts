import type { Session } from "./types.js";

const STORAGE_KEY = "job-search-session";

export function saveSession(session: Session): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function loadSession(): Session | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw) as Session;
    } catch {
        return null;
    }
}

export function clearSession(): void {
    sessionStorage.removeItem(STORAGE_KEY);
}
