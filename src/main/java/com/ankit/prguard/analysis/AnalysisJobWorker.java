package com.ankit.prguard.analysis;

import com.ankit.prguard.decision.DecisionEngine;
import com.ankit.prguard.narrative.NarrativeGenerator;
import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PrGuardProperties;
import com.ankit.prguard.shared.PullRequestSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
class AnalysisJobWorker {
    private final AnalysisJobStore store;
    private final DecisionEngine decisionEngine;
    private final NarrativeGenerator narrativeGenerator;
    private final double confidenceThreshold;
    private final MeterRegistry metrics;

    AnalysisJobWorker(AnalysisJobStore store, DecisionEngine decisionEngine,
            NarrativeGenerator narrativeGenerator, PrGuardProperties properties,
            MeterRegistry metrics) {
        this.store = store;
        this.decisionEngine = decisionEngine;
        this.narrativeGenerator = narrativeGenerator;
        this.confidenceThreshold = properties.decision().confidenceThreshold();
        this.metrics = metrics;
    }

    @Async("analysisExecutor")
    public void process(UUID id) {
        if (store.claim(id) != 1) return;
        metrics.counter("prguard.analysis.started").increment();
        Instant start = Instant.now();
        try {
            PullRequestSnapshot snapshot = store.snapshot(id);
            DecisionResult decision = decisionEngine.evaluate(snapshot);
            boolean review = decision.shouldEscalate(confidenceThreshold);
            String narrative = narrativeGenerator.generate(snapshot, decision);
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            store.complete(id, decision, review, narrative, durationMs);
            Timer.builder("prguard.analysis.duration").register(metrics)
                    .record(Duration.ofMillis(durationMs));
            metrics.counter("prguard.analysis.completed").increment();
        } catch (Exception exception) {
            store.fail(id, exception.getClass().getSimpleName());
            metrics.counter("prguard.analysis.failed").increment();
        }
    }
}
