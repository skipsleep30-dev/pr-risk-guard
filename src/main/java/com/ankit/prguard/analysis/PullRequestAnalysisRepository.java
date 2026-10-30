package com.ankit.prguard.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface PullRequestAnalysisRepository extends JpaRepository<PullRequestAnalysis, UUID> {
    @Transactional (readOnly = true)
    List<PullRequestAnalysis> findTop50ByOwnerOrderByCreatedAtDesc(String owner);
    @Transactional (readOnly = true)
    Optional<PullRequestAnalysis> findByIdAndOwner(UUID id, String owner);
    @Transactional (readOnly = true)
    Optional<PullRequestAnalysis> findByOwnerAndRequestKey(String owner, String requestKey);
    List<PullRequestAnalysis> findByStatus(JobStatus status);

    @Modifying
    @Query("update PullRequestAnalysis a set a.status = :running, a.startedAt = :now " +
            "where a.id = :id and a.status = :pending")
    int claim(@Param("id") UUID id, @Param("pending") JobStatus pending,
            @Param("running") JobStatus running, @Param("now") Instant now);
}
