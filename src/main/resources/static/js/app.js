import * as api from "./api.js";
import { ApiError } from "./api.js";
import { saveSession, loadSession, clearSession } from "./state.js";
const el = (id) => {
    const found = document.getElementById(id);
    if (!found) {
        throw new Error(`Elemento #${id} nao encontrado`);
    }
    return found;
};
const authScreen = el("auth-screen");
const dashboard = el("dashboard");
const userInfo = el("user-info");
const userNameLabel = el("user-name-label");
const alertBox = el("alert-box");
const tabLogin = el("tab-login");
const tabRegister = el("tab-register");
const loginForm = el("login-form");
const registerForm = el("register-form");
const registerTechnologiesBox = el("register-technologies");
const logoutButton = el("logout-button");
const currentTechnologiesBox = el("current-technologies");
const addTechForm = el("add-tech-form");
const addTechnologiesBox = el("add-technologies");
const resumeForm = el("resume-form");
const resumeFileInput = el("resume-file");
const resumeStatus = el("resume-status");
const resumePreview = el("resume-preview");
const resumeText = el("resume-text");
const downloadResumeButton = el("download-resume-button");
const matchesList = el("matches-list");
const refreshMatchesButton = el("refresh-matches-button");
const matchesCountLabel = el("matches-count");
const loadMoreMatchesButton = el("load-more-matches-button");
const MATCHES_PAGE_SIZE = 20;
let session = null;
let allTechnologies = [];
let matchesNextPage = 0;
let matchesTotalElements = 0;
function showError(message) {
    alertBox.textContent = message;
    alertBox.className = "alert alert-error";
}
function showSuccess(message) {
    alertBox.textContent = message;
    alertBox.className = "alert alert-success";
}
function clearAlert() {
    alertBox.className = "alert hidden";
    alertBox.textContent = "";
}
function describeError(error) {
    if (error instanceof ApiError) {
        return error.message;
    }
    return "Nao foi possivel completar a acao. Tente novamente.";
}
function switchTab(tab) {
    const isLogin = tab === "login";
    tabLogin.classList.toggle("tab-active", isLogin);
    tabRegister.classList.toggle("tab-active", !isLogin);
    loginForm.classList.toggle("hidden", !isLogin);
    registerForm.classList.toggle("hidden", isLogin);
    clearAlert();
}
function renderTechnologyCheckboxes(container, technologies, namePrefix) {
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
function getCheckedValues(container) {
    const checkboxes = container.querySelectorAll('input[type="checkbox"]:checked');
    return Array.from(checkboxes).map((checkbox) => checkbox.value);
}
async function loadAllTechnologies() {
    allTechnologies = await api.listTechnologies();
}
function showAuthScreen() {
    authScreen.classList.remove("hidden");
    dashboard.classList.add("hidden");
    userInfo.classList.add("hidden");
}
function showDashboard() {
    authScreen.classList.add("hidden");
    dashboard.classList.remove("hidden");
    userInfo.classList.remove("hidden");
}
async function enterDashboard(newSession) {
    session = newSession;
    saveSession(newSession);
    userNameLabel.textContent = `${newSession.name} (${newSession.email})`;
    showDashboard();
    clearAlert();
    await Promise.all([refreshUserTechnologies(), refreshResume(), refreshMatches()]);
}
async function refreshUserTechnologies() {
    if (!session)
        return;
    const user = await api.getUser(session.userId, session.token);
    currentTechnologiesBox.innerHTML = "";
    if (user.technologies.length === 0) {
        currentTechnologiesBox.innerHTML =
            '<span class="empty-state">Nenhuma tecnologia cadastrada ainda.</span>';
    }
    else {
        for (const name of user.technologies) {
            const tag = document.createElement("span");
            tag.className = "tag";
            tag.textContent = name;
            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "tag-remove";
            removeButton.textContent = "×";
            removeButton.title = `Remover ${name}`;
            removeButton.addEventListener("click", () => handleRemoveTechnology(name));
            tag.appendChild(removeButton);
            currentTechnologiesBox.appendChild(tag);
        }
    }
    const remaining = allTechnologies.filter((tech) => !user.technologies.includes(tech.name));
    renderTechnologyCheckboxes(addTechnologiesBox, remaining, "add-tech");
}
async function handleRemoveTechnology(technologyName) {
    clearAlert();
    if (!session)
        return;
    try {
        await api.removeTechnology(session.userId, session.token, technologyName);
        await refreshUserTechnologies();
    }
    catch (error) {
        showError(describeError(error));
    }
}
async function refreshResume() {
    if (!session)
        return;
    const resume = await api.getResume(session.userId, session.token);
    if (!resume) {
        resumeStatus.textContent = "Nenhum currículo enviado ainda.";
        downloadResumeButton.classList.add("hidden");
        resumePreview.classList.add("hidden");
        return;
    }
    const uploadedAt = new Date(resume.uploadAt).toLocaleString("pt-BR");
    resumeStatus.textContent = `Enviado: ${resume.originalFileName} (${uploadedAt})`;
    downloadResumeButton.classList.remove("hidden");
    resumeText.textContent = resume.extractText || "(nenhum texto extraído)";
    resumePreview.classList.remove("hidden");
}
function renderMatches(matches, append) {
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
            .map((tech) => `<span class="tag">${escapeHtml(tech)}</span>`)
            .join("")}
            </div>
            <a href="${escapeHtml(match.sourceUrlJob)}" target="_blank" rel="noopener">Ver vaga original</a>
        `;
        matchesList.appendChild(item);
    }
}
function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value;
    return div.innerHTML;
}
function updateMatchesFooter() {
    matchesCountLabel.textContent =
        matchesTotalElements === 0 ? "" : `Mostrando ${Math.min(matchesNextPage * MATCHES_PAGE_SIZE, matchesTotalElements)} de ${matchesTotalElements}`;
    loadMoreMatchesButton.classList.toggle("hidden", matchesNextPage * MATCHES_PAGE_SIZE >= matchesTotalElements);
}
async function refreshMatches() {
    if (!session)
        return;
    const result = await api.getMatches(session.userId, session.token, 0, MATCHES_PAGE_SIZE);
    matchesNextPage = 1;
    matchesTotalElements = result.totalElements;
    renderMatches(result.content, false);
    updateMatchesFooter();
}
async function loadMoreMatches() {
    if (!session)
        return;
    const result = await api.getMatches(session.userId, session.token, matchesNextPage, MATCHES_PAGE_SIZE);
    matchesNextPage += 1;
    matchesTotalElements = result.totalElements;
    renderMatches(result.content, true);
    updateMatchesFooter();
}
function doLogout() {
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
    const email = el("login-email").value;
    const password = el("login-password").value;
    try {
        const response = await api.login(email, password);
        await enterDashboard({
            token: response.token,
            userId: response.userId,
            email: response.email,
            name: response.name,
        });
    }
    catch (error) {
        showError(describeError(error));
    }
});
registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    const email = el("register-email").value;
    const name = el("register-name").value;
    const password = el("register-password").value;
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
    }
    catch (error) {
        showError(describeError(error));
    }
});
logoutButton.addEventListener("click", doLogout);
addTechForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearAlert();
    if (!session)
        return;
    const technologies = getCheckedValues(addTechnologiesBox);
    if (technologies.length === 0) {
        showError("Selecione ao menos uma tecnologia.");
        return;
    }
    const submitButton = addTechForm.querySelector('button[type="submit"]');
    if (submitButton)
        submitButton.disabled = true;
    try {
        await api.addTechnologies(session.userId, session.token, technologies);
        await refreshUserTechnologies();
        showSuccess("Tecnologias adicionadas.");
    }
    catch (error) {
        showError(describeError(error));
    }
    finally {
        if (submitButton)
            submitButton.disabled = false;
    }
});
function describeResumeUpload(resume) {
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
    if (!session)
        return;
    const file = resumeFileInput.files?.[0];
    if (!file) {
        showError("Selecione um arquivo PDF.");
        return;
    }
    try {
        const uploaded = await api.uploadResume(session.userId, session.token, file);
        await Promise.all([refreshResume(), refreshUserTechnologies(), refreshMatches()]);
        showSuccess(describeResumeUpload(uploaded));
        resumeForm.reset();
    }
    catch (error) {
        showError(describeError(error));
    }
});
downloadResumeButton.addEventListener("click", async () => {
    clearAlert();
    if (!session)
        return;
    try {
        const { blob, filename } = await api.downloadResume(session.userId, session.token);
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
    }
    catch (error) {
        showError(describeError(error));
    }
});
loadMoreMatchesButton.addEventListener("click", async () => {
    clearAlert();
    try {
        await loadMoreMatches();
    }
    catch (error) {
        showError(describeError(error));
    }
});
refreshMatchesButton.addEventListener("click", async () => {
    clearAlert();
    try {
        await refreshMatches();
    }
    catch (error) {
        showError(describeError(error));
    }
});
async function init() {
    await loadAllTechnologies();
    renderTechnologyCheckboxes(registerTechnologiesBox, allTechnologies, "register-tech");
    const existing = loadSession();
    if (existing) {
        try {
            await enterDashboard(existing);
            return;
        }
        catch {
            clearSession();
        }
    }
    showAuthScreen();
}
init().catch((error) => {
    console.error(error);
    showError("Falha ao carregar a aplicação.");
});
