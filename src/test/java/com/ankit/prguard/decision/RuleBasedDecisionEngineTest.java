package com.ankit.prguard.decision;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.ankit.prguard.shared.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedDecisionEngineTest {

    private final RuleBasedDecisionEngine engine = new RuleBasedDecisionEngine();

    @Test
    void classifiesSmallTestedChangeAsLowRisk() {
        PullRequestSnapshot pullRequest = new PullRequestSnapshot(
                "acme/catalog",
                12,
                "Correct product label",
                "Small display fix",
                "developer",
                12,
                3,
                2,
                List.of("src/CatalogService.java", "test/CatalogServiceTest.java"),
                true
        );

        DecisionResult result = engine.evaluate(pullRequest);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.testAdequacyScore()).isEqualTo(3.0);
        assertThat(result.shouldEscalate(0.75)).isFalse();
    }

    @Test
    void escalatesLargeUntestedSecurityChange() {
        PullRequestSnapshot pullRequest = new PullRequestSnapshot(
                "acme/identity",
                99,
                "Rewrite token verification",
                "Replaces authorization path",
                "developer",
                800,
                100,
                18,
                List.of("src/security/TokenVerifier.java", "src/auth/PermissionService.java"),
                false
        );

        DecisionResult result = engine.evaluate(pullRequest);

        assertThat(result.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.securitySensitiveProbability()).isGreaterThan(0.9);
        assertThat(result.shouldEscalate(0.75)).isTrue();
    }
}
