package com.ankit.prguard.github;

import com.ankit.prguard.shared.PrGuardProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    @Test
    void verifiesGitHubPublishedTestVector() {
        PrGuardProperties properties = properties("It's a Secret to Everybody", true);
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(properties);

        boolean valid = verifier.isValid(
                "Hello, World!".getBytes(StandardCharsets.UTF_8),
                "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17"
        );

        assertThat(valid).isTrue();
    }

    @Test
    void rejectsModifiedPayload() {
        PrGuardProperties properties = properties("It's a Secret to Everybody", true);
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(properties);

        boolean valid = verifier.isValid(
                "Modified".getBytes(StandardCharsets.UTF_8),
                "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17"
        );

        assertThat(valid).isFalse();
    }

    private PrGuardProperties properties(String secret, boolean required) {
        return new PrGuardProperties(
                new PrGuardProperties.GitHub(secret, required, ""),
                new PrGuardProperties.Decision("rules", 0.75),
                new PrGuardProperties.Jev("https://api.typesafe.ai/v1/systemone", "", "jev-latest"),
                new PrGuardProperties.Narrative("template", "", "", "")
        );
    }
}
