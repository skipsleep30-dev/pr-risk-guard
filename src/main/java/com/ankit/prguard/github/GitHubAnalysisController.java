package com.ankit.prguard.github;

import com.ankit.prguard.analysis.AnalysisService;
import com.ankit.prguard.analysis.AnalysisView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/github")
public class GitHubAnalysisController {
    private static final Pattern PR_PATH = Pattern.compile(
            "^/([A-Za-z0-9-]+)/([A-Za-z0-9._-]+)/pull/([1-9][0-9]*)/?$"
    );

    private final GitHubPullRequestClient githubClient;
    private final AnalysisService analysisService;

    public GitHubAnalysisController(
            GitHubPullRequestClient githubClient,
            AnalysisService analysisService) {
        this.githubClient = githubClient;
        this.analysisService = analysisService;
    }

    public record AnalyzeRequest(@NotBlank String url) {}

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisView analyze(@Valid @RequestBody AnalyzeRequest request) {
        URI uri;
        try {
            uri = URI.create(request.url());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid PR URL", exception);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide a github.com PR URL");
        }

        Matcher match = PR_PATH.matcher(uri.getPath());
        if (!match.matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Expected /OWNER/REPO/pull/NUMBER");
        }

        int number;
        try {
            number = Integer.parseInt(match.group(3));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid PR number", exception);
        }

        return analysisService.analyze(
                githubClient.fetch(match.group(1), match.group(2), number));
    }
}