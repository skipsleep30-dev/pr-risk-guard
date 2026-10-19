package com.ankit.prguard.analysis;

import com.ankit.prguard.shared.PullRequestSnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record SubmitAnalysisRequest(
        @NotBlank String repository,
        @Positive int number,
        @NotBlank String title,
        String description,
        String author,
        @PositiveOrZero int additions,
        @PositiveOrZero int deletions,
        @PositiveOrZero int changedFiles,
        List<String> files,
        boolean testsPresent
) {
    PullRequestSnapshot toSnapshot() {
        return new PullRequestSnapshot(
                repository,
                number,
                title,
                description,
                author,
                additions,
                deletions,
                changedFiles,
                files,
                testsPresent
        );
    }
}
