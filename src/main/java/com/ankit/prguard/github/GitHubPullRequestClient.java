package com.ankit.prguard.github;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ankit.prguard.shared.PrGuardProperties;
import com.ankit.prguard.shared.PullRequestSnapshot;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class GitHubPullRequestClient {
    private final RestClient api;
    private final String token;

    public GitHubPullRequestClient(
            RestClient.Builder builder, PrGuardProperties properties) {
        this.api = builder.baseUrl("https://api.github.com/").defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("User-Agent", "pr-risk-guard")
                .build();
        this.token = properties.github().token();
    }

    public PullRequestSnapshot fetch(String owner, String repo, int number) {
        JsonNode pr = get("/repos/{owner}/{repo}/pulls/{number}",owner,repo,number);

        List<String> filenames = new ArrayList<>();

        for(int page = 1; page<=30; page++) {
            JsonNode batch = get("/repos/{owner}/{repo}/pulls/{number}/files?per_page=100&page={page}",
                owner,repo,number,page
            );
            if(!batch.isArray()){
                throw new IllegalStateException("Github return invalid file data");
            }
            batch.forEach(file -> filenames.add(file.path("filename").asText()));

            if(batch.size() < 100) {
                break;
            }
        }
        if(filenames.size() < pr.path("changed_files").asInt()) {
            throw new IllegalStateException("File list is incomplete");
        }

        boolean testsPresent = filenames.stream()
                                .map(name -> name.toLowerCase(Locale.ROOT))
                                .anyMatch(name -> name.contains("test") || name.contains("spec"));
        return new PullRequestSnapshot(
            owner + "/" + repo,
            pr.path("number").asInt(),
                pr.path("title").asText(),
                pr.path("body").asText(""),
                pr.path("user").path("login").asText("unknown"),
                pr.path("additions").asInt(),
                pr.path("deletions").asInt(),
                pr.path("changed_files").asInt(),
                filenames,
                testsPresent
        );         
    }

    public JsonNode get(String path, Object... variables) {
        RestClient.RequestHeadersSpec<?> request = api.get().uri(path, variables);

        if (token != null && !token.isBlank()) {
            request.headers(headers -> headers.setBearerAuth(token));
        }

        JsonNode body = request.retrieve().body(JsonNode.class);
        if (body == null) {
            throw new IllegalStateException("Github returned empty response");
        }
        return body;
    }

}
