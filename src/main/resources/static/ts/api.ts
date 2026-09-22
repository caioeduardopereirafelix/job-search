import type {
    Technology,
    UserResponse,
    LoginResponse,
    JobMatchResponse,
    PagedResponse,
    ResumeResponse,
} from "./types.js";

export class ApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

async function request<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const response = await fetch(path, options);

    if (response.status === 204) {
        return undefined as T;
    }

    const contentType = response.headers.get("content-type") ?? "";
    const body = contentType.includes("application/json")
        ? await response.json()
        : undefined;

    if (!response.ok) {
        const message =
            body && typeof body.message === "string"
                ? body.message
                : `Erro ${response.status}`;
        throw new ApiError(response.status, message);
    }

    return body as T;
}

function authHeaders(token: string): HeadersInit {
    return { Authorization: `Bearer ${token}` };
}

export function listTechnologies(): Promise<Technology[]> {
    return request<Technology[]>("/api/technologies");
}

export function register(
    email: string,
    name: string,
    password: string,
    technologies: string[]
): Promise<UserResponse> {
    return request<UserResponse>("/api/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, name, password, technologies }),
    });
}

export function login(email: string, password: string): Promise<LoginResponse> {
    return request<LoginResponse>("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });
}

export function getUser(id: string, token: string): Promise<UserResponse> {
    return request<UserResponse>(`/api/users/${id}`, {
        headers: authHeaders(token),
    });
}

export function addTechnologies(
    id: string,
    token: string,
    technologies: string[]
): Promise<UserResponse> {
    return request<UserResponse>(`/api/users/${id}/technologies`, {
        method: "POST",
        headers: { ...authHeaders(token), "Content-Type": "application/json" },
        body: JSON.stringify({ technologies }),
    });
}

export function removeTechnology(
    id: string,
    token: string,
    technologyName: string
): Promise<UserResponse> {
    return request<UserResponse>(
        `/api/users/${id}/technologies/${encodeURIComponent(technologyName)}`,
        {
            method: "DELETE",
            headers: authHeaders(token),
        }
    );
}

export function getMatches(
    id: string,
    token: string,
    page: number = 0,
    size: number = 20
): Promise<PagedResponse<JobMatchResponse>> {
    return request<PagedResponse<JobMatchResponse>>(
        `/api/users/${id}/matches?page=${page}&size=${size}`,
        { headers: authHeaders(token) }
    );
}

export async function getResume(
    id: string,
    token: string
): Promise<ResumeResponse | null> {
    try {
        return await request<ResumeResponse>(`/api/users/${id}/resume`, {
            headers: authHeaders(token),
        });
    } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
            return null;
        }
        throw error;
    }
}

export function uploadResume(
    id: string,
    token: string,
    file: File
): Promise<ResumeResponse> {
    const formData = new FormData();
    formData.append("file", file);
    return request<ResumeResponse>(`/api/users/${id}/resume`, {
        method: "POST",
        headers: authHeaders(token),
        body: formData,
    });
}

export async function downloadResume(
    id: string,
    token: string
): Promise<{ blob: Blob; filename: string }> {
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