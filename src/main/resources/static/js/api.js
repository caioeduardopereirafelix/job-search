export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}
async function request(path, options = {}) {
    const response = await fetch(path, options);
    if (response.status === 204) {
        return undefined;
    }
    const contentType = response.headers.get("content-type") ?? "";
    const body = contentType.includes("application/json")
        ? await response.json()
        : undefined;
    if (!response.ok) {
        const message = body && typeof body.message === "string"
            ? body.message
            : `Erro ${response.status}`;
        throw new ApiError(response.status, message);
    }
    return body;
}
function authHeaders(token) {
    return { Authorization: `Bearer ${token}` };
}
export function listTechnologies() {
    return request("/api/technologies");
}
export function register(email, name, password, technologies) {
    return request("/api/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, name, password, technologies }),
    });
}
export function login(email, password) {
    return request("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });
}
export function getUser(id, token) {
    return request(`/api/users/${id}`, {
        headers: authHeaders(token),
    });
}
export function addTechnologies(id, token, technologies) {
    return request(`/api/users/${id}/technologies`, {
        method: "POST",
        headers: { ...authHeaders(token), "Content-Type": "application/json" },
        body: JSON.stringify({ technologies }),
    });
}
export function getMatches(id, token) {
    return request(`/api/users/${id}/matches`, {
        headers: authHeaders(token),
    });
}
export async function getResume(id, token) {
    try {
        return await request(`/api/users/${id}/resume`, {
            headers: authHeaders(token),
        });
    }
    catch (error) {
        if (error instanceof ApiError && error.status === 404) {
            return null;
        }
        throw error;
    }
}
export function uploadResume(id, token, file) {
    const formData = new FormData();
    formData.append("file", file);
    return request(`/api/users/${id}/resume`, {
        method: "POST",
        headers: authHeaders(token),
        body: formData,
    });
}
export async function downloadResume(id, token) {
    const response = await fetch(`/api/users/${id}/resume/download`, {
        headers: authHeaders(token),
    });
    if (!response.ok) {
        throw new ApiError(response.status, `Erro ${response.status} ao baixar o curriculo`);
    }
    const disposition = response.headers.get("content-disposition") ?? "";
    const match = disposition.match(/filename="?([^"]+)"?/);
    const filename = match ? decodeURIComponent(match[1]) : "curriculo.pdf";
    const blob = await response.blob();
    return { blob, filename };
}
