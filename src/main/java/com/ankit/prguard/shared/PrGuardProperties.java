package com.ankit.prguard.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prguard")
public record PrGuardProperties(
        GitHub github,
        Decision decision,
        Jev jev,
        Narrative narrative
) {
    public record GitHub(String webhookSecret, boolean requireSignature, String token) {}

    public record Decision(String provider, double confidenceThreshold) {}

    public record Jev(String apiUrl, String apiKey, String model) {}

    public record Narrative(String provider, String apiUrl, String apiKey, String model) {}
}
