package com.ankit.prguard.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DecisionResult(
        RiskLevel riskLevel,
        double confidence,
        double securitySensitiveProbability,
        double manualReviewProbability,
        double testAdequacyScore,
        String model,
        Map<String, Double> riskProbabilities
) {
    public DecisionResult {
        Objects.requireNonNull(riskLevel, "riskLevel");
        model = Objects.requireNonNullElse(model, "unknown");
        riskProbabilities = Map.copyOf(Objects.requireNonNullElse(riskProbabilities, Map.of()));
        requireProbability(confidence, "confidence");
        requireProbability(securitySensitiveProbability, "securitySensitiveProbability");
        requireProbability(manualReviewProbability, "manualReviewProbability");
        if (testAdequacyScore < 0 || testAdequacyScore > 3) {
            throw new IllegalArgumentException("testAdequacyScore must be between 0 and 3");
        }
    }

    public List<String> escalationReasons(double confidenceThreshold) {
        List<String> reasons = new ArrayList<>();

        if (confidence < confidenceThreshold) {
            reasons.add(
                    "Decision confidence is below the configured threshold."
            );
        }

        if (riskLevel.requiresHumanReview()) {
            reasons.add(
                    riskLevel + " risk requires human review."
            );
        }

        if (securitySensitiveProbability >= 0.70) {
            reasons.add(
                    "Estimated security sensitivity is at or above 70%."
            );
        }

        if (manualReviewProbability >= 0.70) {
            reasons.add(
                    "The decision engine's manual-review estimate is at or above 70%."
            );
        }

        return List.copyOf(reasons);
    }

    public boolean shouldEscalate(double confidenceThreshold) {
        return !escalationReasons(confidenceThreshold).isEmpty();
    }

    private static void requireProbability(double value, String field) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
