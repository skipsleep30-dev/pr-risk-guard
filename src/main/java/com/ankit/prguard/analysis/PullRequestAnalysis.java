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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pull_request_analysis", uniqueConstraints =
        @UniqueConstraint(name = "uq_analysis_owner_key", columnNames = {"owner", "request_key"}))
class PullRequestAnalysis {

    @Id
    private UUID id;

    @Column
    private String owner;

    @Column(name = "request_key", length = 128)
    private String requestKey;

    @Column(length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column
    private JobStatus status;

    @Lob
    private String description;

    private String errorCode;
    private Instant startedAt;
    private Instant finishedAt;

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
    private RiskLevel riskLevel;

    private double confidence;
    private double securitySensitiveProbability;
    private double manualReviewProbability;
    private double testAdequacyScore;
    private boolean humanReviewRequired;

    private String decisionModel;

    @Column(length = 2000)
    private String narrative;

    @Column(nullable = false)
    private Instant createdAt;

    private long durationMs;

    protected PullRequestAnalysis() {
    }

    static PullRequestAnalysis pending(String owner, String requestKey,
            String fingerprint, PullRequestSnapshot pullRequest) {
        PullRequestAnalysis analysis = new PullRequestAnalysis();
        analysis.id = UUID.randomUUID();
        analysis.owner = owner;
        analysis.requestKey = requestKey;
        analysis.requestFingerprint = fingerprint;
        analysis.status = JobStatus.PENDING;
        analysis.repository = pullRequest.repository();
        analysis.pullRequestNumber = pullRequest.number();
        analysis.title = pullRequest.title();
        analysis.description = pullRequest.description();
        analysis.author = pullRequest.author();
        analysis.additions = pullRequest.additions();
        analysis.deletions = pullRequest.deletions();
        analysis.changedFiles = pullRequest.changedFiles();
        analysis.testsPresent = pullRequest.testsPresent();
        analysis.files = String.join("\n", pullRequest.files());
        analysis.createdAt = Instant.now();
        return analysis;
    }

    void markCompleted(DecisionResult decision, boolean humanReviewRequired,
            String narrative, long durationMs) {
        this.status = JobStatus.COMPLETED;
        this.riskLevel = decision.riskLevel();
        this.confidence = decision.confidence();
        this.securitySensitiveProbability = decision.securitySensitiveProbability();
        this.manualReviewProbability = decision.manualReviewProbability();
        this.testAdequacyScore = decision.testAdequacyScore();
        this.humanReviewRequired = humanReviewRequired;
        this.decisionModel = decision.model();
        this.narrative = narrative;
        this.durationMs = durationMs;
        this.finishedAt = Instant.now();
    }

    void markFailed(String errorCode) {
        this.status = JobStatus.FAILED;
        this.errorCode = errorCode;
        this.finishedAt = Instant.now();
    }

    void retry() {
        this.status = JobStatus.PENDING;
        this.errorCode = null;
        this.startedAt = null;
        this.finishedAt = null;
    }

    String requestFingerprint() { return requestFingerprint; }
    JobStatus status() { return status; }
    UUID id() { return id; }

    PullRequestSnapshot toSnapshot() {
        return new PullRequestSnapshot(repository, pullRequestNumber, title, description,
                author, additions, deletions, changedFiles,
                files == null || files.isBlank() ? List.of() : files.lines().toList(), testsPresent);
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
                durationMs,
                status,
                errorCode,
                startedAt,
                finishedAt
        );
    }
}
