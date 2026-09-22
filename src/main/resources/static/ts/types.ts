export interface Technology {
    id: string;
    name: string;
    category: string;
}

export interface UserResponse {
    id: string;
    email: string;
    name: string;
    technologies: string[];
    createdAt: string;
}

export interface LoginResponse {
    token: string;
    userId: string;
    email: string;
    name: string;
}

export interface JobMatchResponse {
    jobId: string;
    titleJob: string;
    companyJob: string;
    sourceUrlJob: string;
    score: number;
    matchedTechnologies: string[];
    createdAt: string;
}

export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface ResumeResponse {
    id: string;
    originalFileName: string;
    extractText: string;
    uploadAt: string;
    /** Só vem preenchido no upload. */
    detectedTechnologies: string[];
    addedTechnologies: string[];
}

export interface Session {
    token: string;
    userId: string;
    email: string;
    name: string;
}
