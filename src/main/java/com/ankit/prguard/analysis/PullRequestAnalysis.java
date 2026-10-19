package com.ankit.prguard.analysis;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.ankit.prguard.shared.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pull_request_analysis")
class PullRequestAnalysis {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String repository;

    @Column(nullable = false)
    private int pullRequestNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private String author;

    private int additions;
    private int deletions;
    private int changedFiles;
    private boolean testsPresent;

    @Lob
    private String files;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    private double confidence;
    private double securitySensitiveProbability;
    private double manualReviewProbability;
    private double testAdequacyScore;
    private boolean humanReviewRequired;

    @Column(nullable = false)
    private String decisionModel;

    @Column(nullable = false, length = 2000)
    private String narrative;

    @Column(nullable = false)
    private Instant createdAt;

    private long durationMs;

    protected PullRequestAnalysis() {
    }

    static PullRequestAnalysis completed(
            PullRequestSnapshot pullRequest,
            DecisionResult decision,
            boolean humanReviewRequired,
            String narrative,
            long durationMs) {
        PullRequestAnalysis analysis = new PullRequestAnalysis();
        analysis.id = UUID.randomUUID();
        analysis.repository = pullRequest.repository();
        analysis.pullRequestNumber = pullRequest.number();
        analysis.title = pullRequest.title();
        analysis.author = pullRequest.author();
        analysis.additions = pullRequest.additions();
        analysis.deletions = pullRequest.deletions();
        analysis.changedFiles = pullRequest.changedFiles();
        analysis.testsPresent = pullRequest.testsPresent();
        analysis.files = String.join("\n", pullRequest.files());
        analysis.riskLevel = decision.riskLevel();
        analysis.confidence = decision.confidence();
        analysis.securitySensitiveProbability = decision.securitySensitiveProbability();
        analysis.manualReviewProbability = decision.manualReviewProbability();
        analysis.testAdequacyScore = decision.testAdequacyScore();
        analysis.humanReviewRequired = humanReviewRequired;
        analysis.decisionModel = decision.model();
        analysis.narrative = narrative;
        analysis.createdAt = Instant.now();
        analysis.durationMs = durationMs;
        return analysis;
    }

    AnalysisView toView() {
        List<String> fileList = files == null || files.isBlank() ? List.of() : files.lines().toList();
        return new AnalysisView(
                id,
                repository,
                pullRequestNumber,
                title,
                author,
                additions,
                deletions,
                changedFiles,
                fileList,
                testsPresent,
                riskLevel,
                confidence,
                securitySensitiveProbability,
                manualReviewProbability,
                testAdequacyScore,
                humanReviewRequired,
                decisionModel,
                narrative,
                createdAt,
                durationMs
        );
    }
}
