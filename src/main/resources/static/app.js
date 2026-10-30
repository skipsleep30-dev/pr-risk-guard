const form = document.querySelector("#analysis-form");
const results = document.querySelector("#results");
const status = document.querySelector("#form-status");
let savedAnalyses = [];
let visibleCount = 5;
let latestAnalysisId = null;

const showMoreButton = document.querySelector("#show-more");

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    status.textContent = "Analyzing…";

    const data = new FormData(form);
    const files = data.get("files").split("\n").map(value => value.trim()).filter(Boolean);
    const payload = {
        repository: data.get("repository"),
        number: Number(data.get("number")),
        title: data.get("title"),
        description: data.get("description"),
        author: "demo-user",
        additions: Number(data.get("additions")),
        deletions: Number(data.get("deletions")),
        changedFiles: Number(data.get("changedFiles")),
        files,
        testsPresent: data.get("testsPresent") === "on"
    };

    try {
        const response = await fetch("/api/analyses", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(await response.text());
        }
        const job = await response.json();
        status.textContent = "Queued; waiting for analysis…";
        showLatestAnalysis(await waitForCompletion(job.id));
        status.textContent = "Analysis completed.";
        await loadAnalyses();
    } catch (error) {
        status.textContent = `Analysis failed: ${error.message}`;
    }
});

document.querySelector("#refresh").addEventListener("click", loadAnalyses);

async function loadAnalyses() {
    results.innerHTML = '<p class="empty">Loading…</p>';
    showMoreButton.hidden = true;

    try {
        const response = await fetch("/api/analyses");
        if (!response.ok) {
            throw new Error(`Request failed (${response.status})`);
        }

        savedAnalyses = await response.json();
        renderHistory();
    } catch (error) {
        results.innerHTML =
            `<p class="empty">${escapeHtml(error.message)}</p>`;
    }
}

function renderHistory() {
    const historyItems = savedAnalyses.filter(
        item => item.id !== latestAnalysisId
    );

    results.innerHTML = historyItems.length
        ? historyItems.slice(0, visibleCount).map(renderAnalysis).join("")
        : '<p class="empty">No previous analyses.</p>';

    showMoreButton.hidden = visibleCount >= historyItems.length;
}

function showLatestAnalysis(analysis) {
    latestAnalysisId = analysis.id;
    visibleCount = 5;

    document.querySelector("#latest-result").innerHTML =
        renderAnalysis(analysis);
    document.querySelector("#latest-section").hidden = false;
}

showMoreButton.addEventListener("click", () => {
    visibleCount += 5;
    renderHistory();
});

function renderAnalysis(item) {
    if (item.status !== "COMPLETED") {
        return `<article class="card"><h3>${escapeHtml(item.title)}</h3>
            <p>${escapeHtml(item.status)}${item.errorCode ? `: ${escapeHtml(item.errorCode)}` : ""}</p></article>`;
    }
    const review = item.humanReviewRequired ? "Human review required" : "Standard review";
    return `
        <article class="card">
            <div class="card-top">
                <div>
                    <h3>${escapeHtml(item.title)}</h3>
                    <div class="meta">${escapeHtml(item.repository)} #${item.pullRequestNumber} · ${escapeHtml(item.author)}</div>
                </div>
                <span class="badge ${item.riskLevel}">${item.riskLevel}</span>
            </div>
            <p>${escapeHtml(item.narrative)}</p>
            <div class="metrics">
                <span class="metric">${review}</span>
                <span class="metric">Confidence ${Math.round(item.confidence * 100)}%</span>
                <span class="metric">Tests ${item.testsPresent ? "present" : "missing"}</span>
                <span class="metric">${escapeHtml(item.decisionModel)}</span>
            </div>
        </article>`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

loadAnalyses();

async function waitForCompletion(id) {
    for (let attempt = 0; attempt < 120; attempt++) {
        const response = await fetch(`/api/analyses/${encodeURIComponent(id)}`);
        if (!response.ok) throw new Error(`Job lookup failed (${response.status})`);
        const job = await response.json();
        if (job.status === "COMPLETED") return job;
        if (job.status === "FAILED") throw new Error(job.errorCode || "Analysis failed");
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    throw new Error("Analysis is still running; refresh the history later.");
}

const githubForm = document.querySelector("#github-form");
const githubStatus = document.querySelector("#github-status");

githubForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = githubForm.querySelector("button");
    const url = new FormData(githubForm).get("url").trim();

    button.disabled = true;
    githubStatus.textContent = "Analyzing pull request…";

    try {
        const response = await fetch("/api/github/analyze", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({url})
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(
                error.message || `Request failed (${response.status})`
            );
        }
        const job = await response.json();
        githubStatus.textContent = "Queued; waiting for analysis…";
        showLatestAnalysis(await waitForCompletion(job.id));
        githubStatus.textContent = "Analysis completed.";
        await loadAnalyses();
    } catch (error) {
        githubStatus.textContent = `Analysis failed: ${error.message}`;
    } finally {
        button.disabled = false;
    }
});
