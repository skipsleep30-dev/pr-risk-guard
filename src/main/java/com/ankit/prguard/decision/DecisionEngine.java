package com.ankit.prguard.decision;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;

public interface DecisionEngine {
    DecisionResult evaluate(PullRequestSnapshot pullRequest);
}
