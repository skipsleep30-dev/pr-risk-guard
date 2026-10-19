package com.ankit.prguard.github;

import com.ankit.prguard.analysis.AnalysisService;
import com.ankit.prguard.analysis.AnalysisView;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/github")
public class GitHubWebhookController {

    private static final Set<String> SUPPORTED_ACTIONS = Set.of("opened", "reopened", "synchronize", "ready_for_review");

    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final AnalysisService analysisService;

    public GitHubWebhookController(
            WebhookSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper,
            AnalysisService analysisService) {
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.analysisService = analysisService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestBody byte[] payload) throws IOException {

        if (!signatureVerifier.isValid(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "rejected", "reason", "invalid webhook signature"));
        }
        if ("ping".equals(event)) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }
        if (!"pull_request".equals(event)) {
            return ResponseEntity.accepted().body(Map.of("status", "ignored", "reason", "unsupported event"));
        }

        JsonNode root = objectMapper.readTree(payload);
        String action = root.path("action").asText();
        if (!SUPPORTED_ACTIONS.contains(action)) {
            return ResponseEntity.accepted().body(Map.of("status", "ignored", "reason", "unsupported action"));
        }

        AnalysisView result = analysisService.analyze(toSnapshot(root));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    private PullRequestSnapshot toSnapshot(JsonNode root) {
        JsonNode pullRequest = root.path("pull_request");
        String title = pullRequest.path("title").asText("Untitled pull request");
        String description = pullRequest.path("body").asText("");
        List<String> files = readOptionalFiles(root.path("prguard_files"));
        boolean testsPresent = root.path("prguard_tests_present").asBoolean(inferTests(title, description, files));

        return new PullRequestSnapshot(
                root.path("repository").path("full_name").asText("unknown/repository"),
                pullRequest.path("number").asInt(root.path("number").asInt()),
                title,
                description,
                pullRequest.path("user").path("login").asText("unknown"),
                pullRequest.path("additions").asInt(),
                pullRequest.path("deletions").asInt(),
                pullRequest.path("changed_files").asInt(files.size()),
                files,
                testsPresent
        );
    }

    private List<String> readOptionalFiles(JsonNode filesNode) {
        if (!filesNode.isArray()) {
            return List.of();
        }
        List<String> files = new ArrayList<>();
        filesNode.forEach(node -> {
            if (node.isTextual() && !node.asText().isBlank()) {
                files.add(node.asText());
            }
        });
        return List.copyOf(files);
    }

    private boolean inferTests(String title, String description, List<String> files) {
        String text = (title + " " + description).toLowerCase(Locale.ROOT);
        return text.contains("test") || files.stream()
                .map(file -> file.toLowerCase(Locale.ROOT))
                .anyMatch(file -> file.contains("test") || file.contains("spec"));
    }
}
