package com.ankit.prguard.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PullRequestAnalysisRepository extends JpaRepository<PullRequestAnalysis, UUID> {
    List<PullRequestAnalysis> findTop50ByOrderByCreatedAtDesc();
}
