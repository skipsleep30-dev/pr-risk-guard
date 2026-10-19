package com.ankit.prguard.narrative;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class TemplateNarrativeGenerator implements NarrativeGenerator {

    @Override
    public String generate(PullRequestSnapshot pullRequest, DecisionResult decision) {
        String testSummary = pullRequest.testsPresent()
                ? "Test-related files were detected."
                : "No test-related files were detected.";
        String securitySummary = decision.securitySensitiveProbability() >= 0.70
                ? "The change appears to touch a security-sensitive area."
                : "No strong security-sensitive signal was detected.";

        return String.format(
                Locale.ROOT,
                "%s risk: %d files and %d changed lines. %s %s Confidence %.0f%%.",
                decision.riskLevel(),
                pullRequest.changedFiles(),
                pullRequest.totalChangedLines(),
                testSummary,
                securitySummary,
                decision.confidence() * 100
        );
    }
}
