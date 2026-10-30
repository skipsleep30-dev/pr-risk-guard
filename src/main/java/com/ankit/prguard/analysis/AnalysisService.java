package com.ankit.prguard.analysis;

import com.ankit.prguard.shared.PullRequestSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {
    private final PullRequestAnalysisRepository repository;
    private final AnalysisJobStore store;
    private final AnalysisJobWorker worker;
    private final ObjectMapper mapper;
    private final MeterRegistry metrics;
    private final Object[] locks = new Object[256];

    public AnalysisService(PullRequestAnalysisRepository repository, AnalysisJobStore store,
            AnalysisJobWorker worker, ObjectMapper mapper, MeterRegistry metrics) {
        this.repository = repository;
        this.store = store;
        this.worker = worker;
        this.mapper = mapper;
        this.metrics = metrics;
        for (int i = 0; i < locks.length; i++) locks[i] = new Object();
    }

    public AnalysisView submit(PullRequestSnapshot snapshot, String owner, String idempotencyKey) {
        String fingerprint = fingerprint(snapshot);
        String key = sha256((idempotencyKey == null || idempotencyKey.isBlank())
                ? fingerprint : idempotencyKey.trim());
        Object lock = locks[Math.floorMod((owner + key).hashCode(), locks.length)];
        synchronized (lock) {
            var existing = repository.findByOwnerAndRequestKey(owner, key);
            if (existing.isPresent()) {
                AnalysisView view = checked(existing.get(), fingerprint);
                metrics.counter("prguard.analysis.deduplicated").increment();
                return view;
            }
            PullRequestAnalysis pending = PullRequestAnalysis.pending(owner, key, fingerprint, snapshot);
            try {
                repository.saveAndFlush(pending);
            } catch (DataIntegrityViolationException collision) {
                PullRequestAnalysis winner = repository.findByOwnerAndRequestKey(owner, key)
                        .orElseThrow(() -> collision);
                AnalysisView view = checked(winner, fingerprint);
                metrics.counter("prguard.analysis.deduplicated").increment();
                return view;
            }
            try {
                worker.process(pending.id());
            } catch (RuntimeException rejected) {
                store.fail(pending.id(), "QUEUE_REJECTED");
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Analysis queue is full");
            }
            return pending.toView();
        }
    }

    @Transactional(readOnly = true)
    public List<AnalysisView> findRecent(String owner) {
        return repository.findTop50ByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(PullRequestAnalysis::toView).toList();
    }

    @Transactional (readOnly = true)
    public AnalysisView findById(UUID id, String owner) {
        return repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"))
                .toView();
    }

    public AnalysisView retry(UUID id, String owner) {
        if (!store.retry(id, owner)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed jobs can be retried");
        }
        try {
            worker.process(id);
        } catch (RuntimeException rejected) {
            store.fail(id, "QUEUE_REJECTED");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Analysis queue is full");
        }
        return findById(id, owner);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        for (UUID id : store.runningIds()) store.fail(id, "INTERRUPTED");
        for (UUID id : store.pendingIds()) {
            try {
                worker.process(id);
            } catch (RuntimeException rejected) {
                store.fail(id, "QUEUE_REJECTED");
            }
        }
    }

    private AnalysisView checked(PullRequestAnalysis existing, String fingerprint) {
        if (!existing.requestFingerprint().equals(fingerprint)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key was already used for different PR data");
        }
        return existing.toView();
    }

    private String fingerprint(PullRequestSnapshot snapshot) {
        try {
            return sha256(mapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize PR snapshot", exception);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
