package com.ankit.prguard.analysis;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class AnalysisJobStore {
    private final PullRequestAnalysisRepository repository;

    AnalysisJobStore(PullRequestAnalysisRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int claim(UUID id) {
        return repository.claim(id, JobStatus.PENDING, JobStatus.RUNNING, Instant.now());
    }

    @Transactional(readOnly = true)
    public PullRequestSnapshot snapshot(UUID id) {
        return repository.findById(id).orElseThrow().toSnapshot();
    }

    @Transactional
    public void complete(UUID id, DecisionResult decision, boolean humanReviewRequired,
            String narrative, long durationMs) {
        PullRequestAnalysis analysis = repository.findById(id).orElseThrow();
        analysis.markCompleted(decision, humanReviewRequired, narrative, durationMs);
    }

    @Transactional
    public void fail(UUID id, String errorCode) {
        PullRequestAnalysis analysis = repository.findById(id).orElseThrow();
        analysis.markFailed(errorCode);
    }

    @Transactional
    public boolean retry(UUID id, String owner) {
        PullRequestAnalysis analysis = repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));
        if (analysis.status() != JobStatus.FAILED) return false;
        analysis.retry();
        return true;
    }

    @Transactional(readOnly = true)
    public List<UUID> pendingIds() {
        return repository.findByStatus(JobStatus.PENDING).stream().map(PullRequestAnalysis::id).toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> runningIds() {
        return repository.findByStatus(JobStatus.RUNNING).stream().map(PullRequestAnalysis::id).toList();
    }
}
