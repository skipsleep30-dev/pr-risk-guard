package com.ankit.prguard.shared;

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

    public boolean shouldEscalate(double confidenceThreshold) {
        return confidence < confidenceThreshold
                || riskLevel.requiresHumanReview()
                || securitySensitiveProbability >= 0.70
                || manualReviewProbability >= 0.70;
    }

    private static void requireProbability(double value, String field) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
