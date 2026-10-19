package com.ankit.prguard.analysis;

import com.ankit.prguard.decision.DecisionEngine;
import com.ankit.prguard.narrative.NarrativeGenerator;
import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PrGuardProperties;
import com.ankit.prguard.shared.PullRequestSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AnalysisService {

    private final DecisionEngine decisionEngine;
    private final NarrativeGenerator narrativeGenerator;
    private final PullRequestAnalysisRepository repository;
    private final double confidenceThreshold;

    public AnalysisService(
            DecisionEngine decisionEngine,
            NarrativeGenerator narrativeGenerator,
            PullRequestAnalysisRepository repository,
            PrGuardProperties properties) {
        this.decisionEngine = decisionEngine;
        this.narrativeGenerator = narrativeGenerator;
        this.repository = repository;
        this.confidenceThreshold = properties.decision().confidenceThreshold();
    }

    @Transactional
    public AnalysisView analyze(PullRequestSnapshot pullRequest) {
        Instant startedAt = Instant.now();
        DecisionResult decision = decisionEngine.evaluate(pullRequest);
        boolean humanReviewRequired = decision.shouldEscalate(confidenceThreshold);
        String narrative = narrativeGenerator.generate(pullRequest, decision);
        long durationMs = Duration.between(startedAt, Instant.now()).toMillis();

        PullRequestAnalysis analysis = PullRequestAnalysis.completed(
                pullRequest,
                decision,
                humanReviewRequired,
                narrative,
                durationMs
        );
        return repository.save(analysis).toView();
    }

    @Transactional(readOnly = true)
    public List<AnalysisView> findRecent() {
        return repository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(PullRequestAnalysis::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisView findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Analysis not found: " + id))
                .toView();
    }
}
