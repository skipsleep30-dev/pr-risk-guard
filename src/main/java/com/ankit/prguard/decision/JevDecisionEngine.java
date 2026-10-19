package com.ankit.prguard.decision;

import com.ankit.prguard.shared.DecisionResult;
import com.ankit.prguard.shared.PrGuardProperties;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.ankit.prguard.shared.RiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "prguard.decision", name = "provider", havingValue = "jev")
public class JevDecisionEngine implements DecisionEngine {

    private final RestClient restClient;
    private final PrGuardProperties.Jev properties;

    public JevDecisionEngine(RestClient.Builder builder, PrGuardProperties properties) {
        this.properties = properties.jev();
        if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
            throw new IllegalStateException("JEV_API_KEY is required when DECISION_PROVIDER=jev");
        }
        this.restClient = builder.build();
    }

    @Override
    public DecisionResult evaluate(PullRequestSnapshot pullRequest) {
        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "state", buildState(pullRequest),
                "questions", buildQuestions()
        );

        try {
            JsonNode response = restClient.post()
                    .uri(properties.apiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new DecisionProviderException("Jev returned an empty response");
            }
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            throw new DecisionProviderException(
                    "Jev request failed with HTTP " + exception.getStatusCode().value(), exception);
        }
    }

    private Map<String, Object> buildState(PullRequestSnapshot pullRequest) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("repository", pullRequest.repository());
        state.put("number", pullRequest.number());
        state.put("title", pullRequest.title());
        state.put("description", pullRequest.description());
        state.put("author", pullRequest.author());
        state.put("additions", pullRequest.additions());
        state.put("deletions", pullRequest.deletions());
        state.put("changed_files", pullRequest.changedFiles());
        state.put("files", pullRequest.files());
        state.put("tests_present", pullRequest.testsPresent());
        return state;
    }

    private Map<String, Object> buildQuestions() {
        Map<String, Object> questions = new LinkedHashMap<>();
        questions.put("risk_level", Map.of(
                "type", "choice",
                "instructions", "Classify the engineering and release risk of this pull request.",
                "criteria", Map.of(
                        "LOW", "Small, isolated change with appropriate tests and no sensitive areas.",
                        "MEDIUM", "Moderate change that deserves normal reviewer attention.",
                        "HIGH", "Large, cross-cutting, security-sensitive, migration, or weakly tested change.",
                        "CRITICAL", "Change could cause severe security, data-loss, payment, or availability impact."
                )
        ));
        questions.put("security_sensitive", Map.of(
                "type", "noul",
                "instructions", "Does this change touch authentication, authorization, secrets, payments, or security controls?",
                "criteria", Map.of(
                        "true", "The change affects a security-sensitive capability.",
                        "false", "The change is unrelated to security-sensitive capabilities."
                )
        ));
        questions.put("test_adequacy", Map.of(
                "type", "score",
                "instructions", "How adequate is the testing evidence for the size and risk of this change?",
                "criteria", List.of("INSUFFICIENT", "PARTIAL", "ADEQUATE", "STRONG")
        ));
        questions.put("manual_review", Map.of(
                "type", "noul",
                "instructions", "Should this pull request require explicit human review before merge?",
                "criteria", Map.of(
                        "true", "Human review is required due to risk or uncertainty.",
                        "false", "Normal automated checks are sufficient."
                )
        ));
        return questions;
    }

    private DecisionResult parseResponse(JsonNode response) {
        JsonNode answers = response.path("answers");
        JsonNode risk = required(answers, "risk_level");
        JsonNode security = required(answers, "security_sensitive");
        JsonNode tests = required(answers, "test_adequacy");
        JsonNode manual = required(answers, "manual_review");

        RiskLevel riskLevel = RiskLevel.valueOf(risk.path("choice").asText().toUpperCase(Locale.ROOT));
        double riskConfidence = risk.path("confidence").asDouble();
        double securityProbability = security.path("noul").asDouble();
        double testScore = tests.path("score").asDouble();
        double testConfidence = tests.path("confidence").asDouble();
        double manualProbability = manual.path("noul").asDouble();

        double confidence = average(
                riskConfidence,
                testConfidence,
                binaryCertainty(securityProbability),
                binaryCertainty(manualProbability)
        );

        Map<String, Double> riskProbabilities = new LinkedHashMap<>();
        risk.path("probabilities").fields().forEachRemaining(
                entry -> riskProbabilities.put(entry.getKey(), entry.getValue().asDouble()));

        return new DecisionResult(
                riskLevel,
                confidence,
                securityProbability,
                manualProbability,
                testScore,
                response.path("model").asText(properties.model()),
                riskProbabilities
        );
    }

    private JsonNode required(JsonNode answers, String name) {
        JsonNode answer = answers.path(name);
        if (answer.isMissingNode()) {
            throw new DecisionProviderException("Jev response is missing answer: " + name);
        }
        return answer;
    }

    private double binaryCertainty(double probability) {
        return Math.abs(probability - 0.5) * 2;
    }

    private double average(double... values) {
        double total = 0;
        for (double value : values) {
            total += value;
        }
        return total / values.length;
    }

    public static class DecisionProviderException extends RuntimeException {
        public DecisionProviderException(String message) {
            super(message);
        }

        public DecisionProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
