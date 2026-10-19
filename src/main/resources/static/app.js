const form = document.querySelector("#analysis-form");
const results = document.querySelector("#results");
const status = document.querySelector("#form-status");

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
        status.textContent = "Analysis completed.";
        await loadAnalyses();
    } catch (error) {
        status.textContent = `Analysis failed: ${error.message}`;
    }
});

document.querySelector("#refresh").addEventListener("click", loadAnalyses);

async function loadAnalyses() {
    results.innerHTML = '<p class="empty">Loading…</p>';
    try {
        const response = await fetch("/api/analyses");
        const analyses = await response.json();
        if (!analyses.length) {
            results.innerHTML = '<p class="empty">No analyses yet. Submit the example above.</p>';
            return;
        }
        results.innerHTML = analyses.map(renderAnalysis).join("");
    } catch (error) {
        results.innerHTML = `<p class="empty">Could not load analyses: ${escapeHtml(error.message)}</p>`;
    }
}

function renderAnalysis(item) {
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
