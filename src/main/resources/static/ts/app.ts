import * as api from "./api.js";
import { ApiError } from "./api.js";
import { saveSession, loadSession, clearSession } from "./state.js";
import type { Session, Technology, JobMatchResponse, ResumeResponse } from "./types.js";

const el = <T extends HTMLElement>(id: string): T => {
    const found = document.getElementById(id);
    if (!found) {
        throw new Error(`Elemento #${id} nao encontrado`);
    }
    return found as T;
};

const authScreen = el<HTMLElement>("auth-screen");
const dashboard = el<HTMLElement>("dashboard");
const userInfo = el<HTMLElement>("user-info");
const userNameLabel = el<HTMLElement>("user-name-label");
const alertBox = el<HTMLElement>("alert-box");

const authHeading = el<HTMLElement>("auth-heading");
const tabLogin = el<HTMLButtonElement>("tab-login");
const tabRegister = el<HTMLButtonElement>("tab-register");
const loginForm = el<HTMLFormElement>("login-form");
const registerForm = el<HTMLFormElement>("register-form");

const logoutButton = el<HTMLButtonElement>("logout-button");
const currentTechnologiesBox = el<HTMLElement>("current-technologies");
const techSearchInput = el<HTMLInputElement>("tech-search-input");
const techSuggestionsBox = el<HTMLElement>("tech-suggestions");

const resumeForm = el<HTMLFormElement>("resume-form");
const resumeFileInput = el<HTMLInputElement>("resume-file");
const resumeStatus = el<HTMLElement>("resume-status");
const resumePreview = el<HTMLElement>("resume-preview");
const resumeText = el<HTMLElement>("resume-text");
const downloadResumeButton = el<HTMLButtonElement>("download-resume-button");
const deleteResumeButton = el<HTMLButtonElement>("delete-resume-button");

const matchesList = el<HTMLElement>("matches-list");
const refreshMatchesButton = el<HTMLButtonElement>("refresh-matches-button");
const matchesCountLabel = el<HTMLElement>("matches-count");
const loadMoreMatchesButton = el<HTMLButtonElement>("load-more-matches-button");

const MATCHES_PAGE_SIZE = 20;

const MAX_SUGGESTIONS = 8;

let session: Session | null = null;
let allTechnologies: Technology[] = [];
let remainingTechnologies: Technology[] = [];
let activeSuggestionIndex = -1;
let matchesNextPage = 0;
let matchesTotalElements = 0;

function showError(message: string): void {
    alertBox.textContent = message;
    alertBox.className = "alert alert-error";
}

function showSuccess(message: string): void {
    alertBox.textContent = message;
    alertBox.className = "alert alert-success";
}

function clearAlert(): void {
    alertBox.className = "alert hidden";
    alertBox.textContent = "";
}

function describeError(error: unknown): string {
    if (error instanceof ApiError) {
        return error.message;
    }
    return "Nao foi possivel completar a acao. Tente novamente.";
}

function clearFieldErrors(form: HTMLFormElement): void {
    form.querySelectorAll<HTMLElement>(".field-error").forEach((el) => {
        el.textContent = "";
    });
}

function setFieldError(fieldId: string, message: string): void {
    const errorEl = document.getElementById(`${fieldId}-error`);
    if (errorEl) errorEl.textContent = message;
}

function applyValidationErrors(form: HTMLFormElement, fieldErrors: string[]): void {
    for (const raw of fieldErrors) {
        const separatorIndex = raw.indexOf(":");
        if (separatorIndex === -1) continue;
        const field = raw.slice(0, separatorIndex).trim();
        const message = raw.slice(separatorIndex + 1).trim();
        setFieldError(`${form.id.replace("-form", "")}-${field}`, message);
    }
}

function setButtonLoading(button: HTMLButtonElement | null, loadingText: string): void {
    if (!button) return;
    button.disabled = true;
    button.dataset.originalText = button.textContent ?? "";
    button.textContent = loadingText;
}

function resetButtonLoading(button: HTMLButtonElement | null): void {
    if (!button) return;
    button.disabled = false;
    button.textContent = button.dataset.originalText ?? button.textContent ?? "";
}

function switchTab(tab: "login" | "register"): void {
    const isLogin = tab === "login";
    authHeading.textContent = isLogin ? "Entrar" : "Cadastrar";
    tabLogin.classList.toggle("tab-active", isLogin);
    tabRegister.classList.toggle("tab-active", !isLogin);
    loginForm.classList.toggle("hidden", !isLogin);
    registerForm.classList.toggle("hidden", isLogin);
    clearAlert();
    clearFieldErrors(loginForm);
    clearFieldErrors(registerForm);
}

function hideSuggestions(): void {
    techSuggestionsBox.classList.add("hidden");
    techSuggestionsBox.innerHTML = "";
    activeSuggestionIndex = -1;
}

function getSuggestionItems(): HTMLButtonElement[] {
    return Array.from(techSuggestionsBox.querySelectorAll<HTMLButtonElement>(".tech-suggestion-item"));
}

function setActiveSuggestion(index: number): void {
    const items = getSuggestionItems();
    if (items.length === 0) {
        activeSuggestionIndex = -1;
        return;
    }

    activeSuggestionIndex = ((index % items.length) + items.length) % items.length;

    items.forEach((item, i) => {
        item.classList.toggle("active", i === activeSuggestionIndex);
    });
    items[activeSuggestionIndex].scrollIntoView({ block: "nearest" });
}

function renderSuggestions(matches: Technology[]): void {
    techSuggestionsBox.innerHTML = "";
    activeSuggestionIndex = -1;

    if (matches.length === 0) {
        const empty = document.createElement("div");
        empty.className = "tech-suggestions-empty";
        empty.textContent = "Nenhuma tecnologia encontrada.";
        techSuggestionsBox.appendChild(empty);
    } else {
        for (const tech of matches.slice(0, MAX_SUGGESTIONS)) {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "tech-suggestion-item";
            item.textContent = tech.name;
            item.addEventListener("click", () => handleAddTechnology(tech.name));
            techSuggestionsBox.appendChild(item);
        }
    }

    techSuggestionsBox.classList.remove("hidden");
}

function handleSearchInput(): void {
    const query = techSearchInput.value.trim().toLowerCase();
    if (!query) {
        hideSuggestions();
        return;
    }

    const matches = remainingTechnologies.filter((tech) =>
        tech.name.toLowerCase().includes(query)
    );
    renderSuggestions(matches);
}

async function handleAddTechnology(technologyName: string): Promise<void> {
    clearAlert();
    if (!session) return;

    try {
        await api.addTechnologies(session.userId, session.token, [technologyName]);
        await refreshUserTechnologies();
        showSuccess(`${technologyName} adicionada.`);
    } catch (error) {
        showError(describeError(error));
    } finally {
        techSearchInput.value = "";
        hideSuggestions();
        techSearchInput.focus();
    }
}

async function loadAllTechnologies(): Promise<void> {
    allTechnologies = await api.listTechnologies();
}

function showAuthScreen(): void {
    authScreen.classList.remove("hidden");
    dashboard.classList.add("hidden");
    userInfo.classList.add("hidden");
}

function showDashboard(): void {
    authScreen.classList.add("hidden");
    dashboard.classList.remove("hidden");
    userInfo.classList.remove("hidden");
}

async function enterDashboard(newSession: Session): Promise<void> {
    session = newSession;
    saveSession(newSession);
    userNameLabel.textContent = `${newSession.name} (${newSession.email})`;
    showDashboard();
    clearAlert();
    await Promise.all([refreshUserTechnologies(), refreshResume(), refreshMatches()]);
}

async function refreshUserTechnologies(): Promise<void> {
    if (!session) return;
    const user = await api.getUser(session.userId, session.token);

    currentTechnologiesBox.innerHTML = "";
    if (user.technologies.length === 0) {
        currentTechnologiesBox.innerHTML =
            '<span class="empty-state">Nenhuma tecnologia cadastrada ainda.</span>';
    } else {
        for (const name of user.technologies) {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = name;

            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "tag-remove";
            removeButton.textContent = "×";
            removeButton.title = `Remover ${name}`;
            removeButton.addEventListener("click", () => handleRemoveTechnology(name, removeButton));

            tag.appendChild(removeButton);
            currentTechnologiesBox.appendChild(tag);
        }
    }

    remainingTechnologies = allTechnologies.filter(
        (tech) => !user.technologies.includes(tech.name)
    );
}

async function handleRemoveTechnology(technologyName: string, button: HTMLButtonElement): Promise<void> {
    if (!window.confirm(`Remover ${technologyName} do seu perfil?`)) {
        return;
    }

    clearAlert();
    if (!session) return;

    button.disabled = true;
    try {
        await api.removeTechnology(session.userId, session.token, technologyName);
        await refreshUserTechnologies();
    } catch (error) {
        showError(describeError(error));
        button.disabled = false;
    }
}

async function refreshResume(): Promise<void> {
    if (!session) return;
    const resume = await api.getResume(session.userId, session.token);

    if (!resume) {
        resumeStatus.textContent = "Nenhum currículo enviado ainda.";
        downloadResumeButton.classList.add("hidden");
        deleteResumeButton.classList.add("hidden");
        resumePreview.classList.add("hidden");
        return;
    }

    const uploadedAt = new Date(resume.uploadAt).toLocaleString("pt-BR");
    resumeStatus.textContent = `Enviado: ${resume.originalFileName} (${uploadedAt})`;
    downloadResumeButton.classList.remove("hidden");
    deleteResumeButton.classList.remove("hidden");
    resumeText.textContent = resume.extractText || "(nenhum texto extraído)";
    resumePreview.classList.remove("hidden");
}

function renderMatches(matches: JobMatchResponse[], append: boolean): void {
    if (!append) {
        matchesList.innerHTML = "";
    }

    if (!append && matches.length === 0) {
        matchesList.innerHTML =
            '<span class="empty-state">Nenhuma vaga compatível encontrada ainda.</span>';
        return;
    }

    for (const match of matches) {
        const item = document.createElement("div");
        item.className = "match-item";

        const scorePercent = Math.round(match.score * 100);

        item.innerHTML = `
            <div class="match-item-header">
                <div>
                    <div class="match-item-title">${escapeHtml(match.titleJob)}</div>
                    <div class="match-item-company">${escapeHtml(match.companyJob ?? "")}</div>
                </div>
                <span class="score-badge">${scorePercent}% match</span>
            </div>
            <div class="tag-list" style="margin-top:8px;">
                ${match.matchedTechnologies
                    .map((tech: string) => `<span class="tag">${escapeHtml(tech)}</span>`)
                    .join("")}
            </div>
            <a href="${escapeHtml(match.sourceUrlJob)}" target="_blank" rel="noopener">Ver vaga original</a>
        `;
        matchesList.appendChild(item);
    }
}

function escapeHtml(value: string): string {
    const div = document.createElement("div");
    div.textContent = value;
    return div.innerHTML;
}

function updateMatchesFooter(): void {
    matchesCountLabel.textContent =
        matchesTotalElements === 0 ? "" : `Mostrando ${Math.min(matchesNextPage * MATCHES_PAGE_SIZE, matchesTotalElements)} de ${matchesTotalElements}`;
    loadMoreMatchesButton.classList.toggle("hidden", matchesNextPage * MATCHES_PAGE_SIZE >= matchesTotalElements);
}

async function refreshMatches(): Promise<void> {
    if (!session) return;
    const result = await api.getMatches(session.userId, session.token, 0, MATCHES_PAGE_SIZE);
    matchesNextPage = 1;
    matchesTotalElements = result.totalElements;
    renderMatches(result.content, false);
    updateMatchesFooter();
}

async function loadMoreMatches(): Promise<void> {
    if (!session) return;
    const result = await api.getMatches(session.userId, session.token, matchesNextPage, MATCHES_PAGE_SIZE);
    matchesNextPage += 1;
    matchesTotalElements = result.totalElements;
    renderMatches(result.content, true);
    updateMatchesFooter();
}

function doLogout(): void {
    session = null;
    clearSession();
    showAuthScreen();
    switchTab("login");
    loginForm.reset();
    registerForm.reset();
}

tabLogin.addEventListener("click", () => switchTab("login"));
tabRegister.addEventListener("click", () => switchTab("register"));

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    clearFieldErrors(loginForm);
    const email = el<HTMLInputElement>("login-email").value;
    const password = el<HTMLInputElement>("login-password").value;

    const submitButton = loginForm.querySelector<HTMLButtonElement>('button[type="submit"]');
    setButtonLoading(submitButton, "Entrando...");

    try {
        const response = await api.login(email, password);
        await enterDashboard({
            token: response.token,
            userId: response.userId,
            email: response.email,
            name: response.name,
        });
    } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
            setFieldError("login-password", error.message);
        } else {
            showError(describeError(error));
        }
    } finally {
        resetButtonLoading(submitButton);
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    clearFieldErrors(registerForm);
    const email = el<HTMLInputElement>("register-email").value;
    const name = el<HTMLInputElement>("register-name").value;
    const password = el<HTMLInputElement>("register-password").value;

    const submitButton = registerForm.querySelector<HTMLButtonElement>('button[type="submit"]');
    setButtonLoading(submitButton, "Cadastrando...");

    try {
        await api.register(email, name, password);
        const loginResponse = await api.login(email, password);
        await enterDashboard({
            token: loginResponse.token,
            userId: loginResponse.userId,
            email: loginResponse.email,
            name: loginResponse.name,
        });
    } catch (error) {
        if (error instanceof ApiError && error.fieldErrors && error.fieldErrors.length > 0) {
            applyValidationErrors(registerForm, error.fieldErrors);
        } else if (error instanceof ApiError && error.status === 409) {
            setFieldError("register-email", error.message);
        } else {
            showError(describeError(error));
        }
    } finally {
        resetButtonLoading(submitButton);
    }
});

logoutButton.addEventListener("click", doLogout);

techSearchInput.addEventListener("input", handleSearchInput);

techSearchInput.addEventListener("keydown", (event) => {
    const items = getSuggestionItems();

    if (event.key === "Escape") {
        hideSuggestions();
    } else if (event.key === "ArrowDown") {
        if (items.length === 0) return;
        event.preventDefault();
        setActiveSuggestion(activeSuggestionIndex + 1);
    } else if (event.key === "ArrowUp") {
        if (items.length === 0) return;
        event.preventDefault();
        setActiveSuggestion(activeSuggestionIndex - 1);
    } else if (event.key === "Enter") {
        event.preventDefault();
        const selected = items[activeSuggestionIndex] ?? items[0];
        selected?.click();
    }
});

document.addEventListener("click", (event) => {
    const target = event.target as Node;
    if (!techSearchInput.contains(target) && !techSuggestionsBox.contains(target)) {
        hideSuggestions();
    }
});

function describeResumeUpload(resume: ResumeResponse): string {
    if (!resume.extractText.trim()) {
        return "Currículo enviado, mas não foi possível ler o texto do PDF (talvez seja uma imagem escaneada).";
    }
    if (resume.addedTechnologies.length > 0) {
        return `Currículo enviado. Tecnologias adicionadas ao perfil: ${resume.addedTechnologies.join(", ")}.`;
    }
    if (resume.detectedTechnologies.length > 0) {
        return "Currículo enviado. As tecnologias encontradas já estavam no seu perfil.";
    }
    return "Currículo enviado. Nenhuma tecnologia do catálogo foi encontrada no texto.";
}

resumeForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    if (!session) return;

    const file = resumeFileInput.files?.[0];
    if (!file) {
        showError("Selecione um arquivo PDF.");
        return;
    }

    const submitButton = resumeForm.querySelector<HTMLButtonElement>('button[type="submit"]');
    setButtonLoading(submitButton, "Enviando...");

    try {
        const uploaded = await api.uploadResume(session.userId, session.token, file);
        await Promise.all([refreshResume(), refreshUserTechnologies(), refreshMatches()]);
        showSuccess(describeResumeUpload(uploaded));
        resumeForm.reset();
    } catch (error) {
        showError(describeError(error));
    } finally {
        resetButtonLoading(submitButton);
    }
});

downloadResumeButton.addEventListener("click", async () => {
    clearAlert();
    if (!session) return;

    setButtonLoading(downloadResumeButton, "Baixando...");
    try {
        const { blob, filename } = await api.downloadResume(session.userId, session.token);
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
    } catch (error) {
        showError(describeError(error));
    } finally {
        resetButtonLoading(downloadResumeButton);
    }
});

deleteResumeButton.addEventListener("click", async () => {
    if (!window.confirm("Excluir o currículo enviado? Essa acao nao pode ser desfeita.")) {
        return;
    }

    clearAlert();
    if (!session) return;

    setButtonLoading(deleteResumeButton, "Excluindo...");
    try {
        await api.deleteResume(session.userId, session.token);
        await refreshResume();
        showSuccess("Currículo excluído.");
    } catch (error) {
        showError(describeError(error));
    } finally {
        resetButtonLoading(deleteResumeButton);
    }
});

loadMoreMatchesButton.addEventListener("click", async () => {
    clearAlert();
    setButtonLoading(loadMoreMatchesButton, "Carregando...");
    try {
        await loadMoreMatches();
    } catch (error) {
        showError(describeError(error));
    } finally {
        resetButtonLoading(loadMoreMatchesButton);
    }
});

refreshMatchesButton.addEventListener("click", async () => {
    clearAlert();
    setButtonLoading(refreshMatchesButton, "Atualizando...");
    try {
        await refreshMatches();
    } catch (error) {
        showError(describeError(error));
    } finally {
        resetButtonLoading(refreshMatchesButton);
    }
});

async function init(): Promise<void> {
    await loadAllTechnologies();

    const existing = loadSession();
    if (existing) {
        try {
            await enterDashboard(existing);
            return;
        } catch {
            clearSession();
        }
    }
    showAuthScreen();
}

init().catch((error) => {
    console.error(error);
    showError("Falha ao carregar a aplicação.");
});