package com.ankit.prguard.decision;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.ankit.prguard.shared.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "prguard.decision", name = "provider", havingValue = "rules", matchIfMissing = true)
public class RuleBasedDecisionEngine implements DecisionEngine {

    @Override
    public DecisionResult evaluate(PullRequestSnapshot pullRequest) {
        boolean sensitive = pullRequest.files().stream().anyMatch(this::isSensitivePath);
        int lines = pullRequest.totalChangedLines();

        RiskLevel risk = calculateRisk(pullRequest, sensitive, lines);
        double confidence = 0.88;
        double securityProbability = sensitive ? 0.92 : 0.08;
        double manualReviewProbability = switch (risk) {
            case CRITICAL -> 0.98;
            case HIGH -> 0.90;
            case MEDIUM -> 0.58;
            case LOW -> 0.18;
        };
        double testAdequacy = calculateTestAdequacy(pullRequest, lines);

        Map<String, Double> probabilities = probabilitiesFor(risk);
        return new DecisionResult(
                risk,
                confidence,
                securityProbability,
                manualReviewProbability,
                testAdequacy,
                "deterministic-rules-v1",
                probabilities
        );
    }

    private RiskLevel calculateRisk(PullRequestSnapshot pullRequest, boolean sensitive, int lines) {
        if (sensitive && lines > 500 && !pullRequest.testsPresent()) {
            return RiskLevel.CRITICAL;
        }
        if (sensitive || lines > 1_000 || pullRequest.changedFiles() > 30) {
            return RiskLevel.HIGH;
        }
        if (lines > 350 || pullRequest.changedFiles() > 12 || (!pullRequest.testsPresent() && lines > 100)) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private double calculateTestAdequacy(PullRequestSnapshot pullRequest, int lines) {
        if (pullRequest.testsPresent() && pullRequest.changedFiles() <= 5) {
            return 3.0;
        }
        if (pullRequest.testsPresent()) {
            return 2.0;
        }
        return lines <= 75 ? 1.0 : 0.0;
    }

    private boolean isSensitivePath(String file) {
        String normalized = file.toLowerCase(Locale.ROOT);
        return normalized.contains("auth")
                || normalized.contains("security")
                || normalized.contains("permission")
                || normalized.contains("migration")
                || normalized.startsWith(".github/workflows/")
                || normalized.contains("payment");
    }

    private Map<String, Double> probabilitiesFor(RiskLevel risk) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (RiskLevel candidate : RiskLevel.values()) {
            values.put(candidate.name(), candidate == risk ? 0.85 : 0.05);
        }
        return values;
    }
}
