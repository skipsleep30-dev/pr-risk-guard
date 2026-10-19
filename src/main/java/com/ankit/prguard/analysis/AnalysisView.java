package com.ankit.prguard.analysis;

import com.ankit.prguard.shared.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisView(
        UUID id,
        String repository,
        int pullRequestNumber,
        String title,
        String author,
        int additions,
        int deletions,
        int changedFiles,
        List<String> files,
        boolean testsPresent,
        RiskLevel riskLevel,
        double confidence,
        double securitySensitiveProbability,
        double manualReviewProbability,
        double testAdequacyScore,
        boolean humanReviewRequired,
        String decisionModel,
        String narrative,
        Instant createdAt,
        long durationMs
) {}
