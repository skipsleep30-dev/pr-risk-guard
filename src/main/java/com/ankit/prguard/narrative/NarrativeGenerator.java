package com.ankit.prguard.narrative;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;

public interface NarrativeGenerator {
    String generate(PullRequestSnapshot pullRequest, DecisionResult decision);
}
