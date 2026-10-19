package com.ankit.prguard.narrative;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PrGuardProperties;
import com.ankit.prguard.shared.PullRequestSnapshot;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.List;

@Component
public class TemplateNarrativeGenerator implements NarrativeGenerator {
    private final double confidenceThreshold;

    public TemplateNarrativeGenerator(PrGuardProperties properties) {
        this.confidenceThreshold = properties.decision().confidenceThreshold();
    }
    @Override
    public String generate(PullRequestSnapshot pullRequest, DecisionResult decision) {
        List<String> testPaths = pullRequest.files().stream()
                .filter(path -> {
                    String lower = path.toLowerCase(Locale.ROOT);
                    return lower.contains("test") || lower.contains("spec");
                })
                .limit(3)
                .map(path -> path.length() > 140
                        ? path.substring(0, 137) + "..."
                        : path)
                .toList();

        String testEvidence = testPaths.isEmpty()
                ? "No paths matching test/spec were found in the supplied filenames."
                : "Example paths matching test/spec: "
                + String.join(", ", testPaths) + ".";

        List<String> reasons = decision.escalationReasons(confidenceThreshold);

        String escalation = reasons.isEmpty()
                ? "Human review is not required by the configured policy."
                : "Human review is required. " + String.join(" ", reasons);

        return String.format(
                Locale.ROOT,
                """
                Risk: %s across %d files and %d changed lines.
                Security sensitivity estimate: %.0f%%.
                Test evidence: %s
                Input indicates tests: %s. Estimated test adequacy: %.1f/3.
                Engine manual-review estimate: %.0f%%.
                Escalation: %s
                Decision confidence score: %.0f%%; configured threshold: %.0f%%.
                Evidence scope: PR metadata and filenames. Code diffs, test execution and coverage were not inspected.
                """,
                decision.riskLevel(),
                pullRequest.changedFiles(),
                pullRequest.totalChangedLines(),
                decision.securitySensitiveProbability() * 100,
                testEvidence,
                pullRequest.testsPresent() ? "yes" : "no",
                decision.testAdequacyScore(),
                decision.manualReviewProbability() * 100,
                escalation,
                decision.confidence() * 100,
                confidenceThreshold * 100
        ).strip();
    }
}
