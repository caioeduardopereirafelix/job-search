import * as api from "./api.js";
import { ApiError } from "./api.js";
import { saveSession, loadSession, clearSession } from "./state.js";
import type { Session, Technology, JobMatchResponse } from "./types.js";

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

const tabLogin = el<HTMLButtonElement>("tab-login");
const tabRegister = el<HTMLButtonElement>("tab-register");
const loginForm = el<HTMLFormElement>("login-form");
const registerForm = el<HTMLFormElement>("register-form");
const registerTechnologiesBox = el<HTMLElement>("register-technologies");

const logoutButton = el<HTMLButtonElement>("logout-button");
const currentTechnologiesBox = el<HTMLElement>("current-technologies");
const addTechForm = el<HTMLFormElement>("add-tech-form");
const addTechnologiesBox = el<HTMLElement>("add-technologies");

const resumeForm = el<HTMLFormElement>("resume-form");
const resumeFileInput = el<HTMLInputElement>("resume-file");
const resumeStatus = el<HTMLElement>("resume-status");
const resumePreview = el<HTMLElement>("resume-preview");
const resumeText = el<HTMLElement>("resume-text");
const downloadResumeButton = el<HTMLButtonElement>("download-resume-button");

const matchesList = el<HTMLElement>("matches-list");
const refreshMatchesButton = el<HTMLButtonElement>("refresh-matches-button");

let session: Session | null = null;
let allTechnologies: Technology[] = [];

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

function switchTab(tab: "login" | "register"): void {
    const isLogin = tab === "login";
    tabLogin.classList.toggle("tab-active", isLogin);
    tabRegister.classList.toggle("tab-active", !isLogin);
    loginForm.classList.toggle("hidden", !isLogin);
    registerForm.classList.toggle("hidden", isLogin);
    clearAlert();
}

function renderTechnologyCheckboxes(
    container: HTMLElement,
    technologies: Technology[],
    namePrefix: string
): void {
    container.innerHTML = "";
    for (const tech of technologies) {
        const label = document.createElement("label");
        label.className = "tech-checkbox";

        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = tech.name;
        checkbox.name = namePrefix;

        label.appendChild(checkbox);
        label.appendChild(document.createTextNode(tech.name));
        container.appendChild(label);
    }
}

function getCheckedValues(container: HTMLElement): string[] {
    const checkboxes = container.querySelectorAll<HTMLInputElement>(
        'input[type="checkbox"]:checked'
    );
    return Array.from(checkboxes).map((checkbox) => checkbox.value);
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
            removeButton.textContent = "Ã—";
            removeButton.title = `Remover ${name}`;
            removeButton.addEventListener("click", () => handleRemoveTechnology(name));

            tag.appendChild(removeButton);
            currentTechnologiesBox.appendChild(tag);
        }
    }

    const remaining = allTechnologies.filter(
        (tech) => !user.technologies.includes(tech.name)
    );
    renderTechnologyCheckboxes(addTechnologiesBox, remaining, "add-tech");
}

async function handleRemoveTechnology(technologyName: string): Promise<void> {
    clearAlert();
    if (!session) return;

    try {
        await api.removeTechnology(session.userId, session.token, technologyName);
        await refreshUserTechnologies();
    } catch (error) {
        showError(describeError(error));
    }
}

async function refreshResume(): Promise<void> {
    if (!session) return;
    const resume = await api.getResume(session.userId, session.token);

    if (!resume) {
        resumeStatus.textContent = "Nenhum currÃ­culo enviado ainda.";
        downloadResumeButton.classList.add("hidden");
        resumePreview.classList.add("hidden");
        return;
    }

    const uploadedAt = new Date(resume.uploadAt).toLocaleString("pt-BR");
    resumeStatus.textContent = `Enviado: ${resume.originalFileName} (${uploadedAt})`;
    downloadResumeButton.classList.remove("hidden");
    resumeText.textContent = resume.extractText || "(nenhum texto extraÃ­do)";
    resumePreview.classList.remove("hidden");
}

function renderMatches(matches: JobMatchResponse[]): void {
    matchesList.innerHTML = "";

    if (matches.length === 0) {
        matchesList.innerHTML =
            '<span class="empty-state">Nenhuma vaga compatÃ­vel encontrada ainda.</span>';
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

async function refreshMatches(): Promise<void> {
    if (!session) return;
    const matches = await api.getMatches(session.userId, session.token);
    renderMatches(matches);
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
    const email = el<HTMLInputElement>("login-email").value;
    const password = el<HTMLInputElement>("login-password").value;

    try {
        const response = await api.login(email, password);
        await enterDashboard({
            token: response.token,
            userId: response.userId,
            email: response.email,
            name: response.name,
        });
    } catch (error) {
        showError(describeError(error));
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    const email = el<HTMLInputElement>("register-email").value;
    const name = el<HTMLInputElement>("register-name").value;
    const password = el<HTMLInputElement>("register-password").value;
    const technologies = getCheckedValues(registerTechnologiesBox);

    try {
        await api.register(email, name, password, technologies);
        const loginResponse = await api.login(email, password);
        await enterDashboard({
            token: loginResponse.token,
            userId: loginResponse.userId,
            email: loginResponse.email,
            name: loginResponse.name,
        });
    } catch (error) {
        showError(describeError(error));
    }
});

logoutButton.addEventListener("click", doLogout);

addTechForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    if (!session) return;

    const technologies = getCheckedValues(addTechnologiesBox);
    if (technologies.length === 0) {
        showError("Selecione ao menos uma tecnologia.");
        return;
    }

    const submitButton = addTechForm.querySelector<HTMLButtonElement>('button[type="submit"]');
    if (submitButton) submitButton.disabled = true;

    try {
        await api.addTechnologies(session.userId, session.token, technologies);
        await refreshUserTechnologies();
        showSuccess("Tecnologias adicionadas.");
    } catch (error) {
        showError(describeError(error));
    } finally {
        if (submitButton) submitButton.disabled = false;
    }
});

resumeForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    if (!session) return;

    const file = resumeFileInput.files?.[0];
    if (!file) {
        showError("Selecione um arquivo PDF.");
        return;
    }

    try {
        await api.uploadResume(session.userId, session.token, file);
        await refreshResume();
        showSuccess("CurrÃ­culo enviado com sucesso.");
        resumeForm.reset();
    } catch (error) {
        showError(describeError(error));
    }
});

downloadResumeButton.addEventListener("click", async () => {
    clearAlert();
    if (!session) return;

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
    }
});

refreshMatchesButton.addEventListener("click", async () => {
    clearAlert();
    try {
        await refreshMatches();
    } catch (error) {
        showError(describeError(error));
    }
});

async function init(): Promise<void> {
    await loadAllTechnologies();
    renderTechnologyCheckboxes(registerTechnologiesBox, allTechnologies, "register-tech");

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
    showError("Falha ao carregar a aplicaÃ§Ã£o.");
});