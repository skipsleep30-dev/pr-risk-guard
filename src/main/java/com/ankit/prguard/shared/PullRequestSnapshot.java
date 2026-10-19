package com.ankit.prguard.shared;

import java.util.List;
import java.util.Objects;

public record PullRequestSnapshot(
        String repository,
        int number,
        String title,
        String description,
        String author,
        int additions,
        int deletions,
        int changedFiles,
        List<String> files,
        boolean testsPresent
) {
    public PullRequestSnapshot {
        repository = requireText(repository, "repository");
        title = requireText(title, "title");
        description = Objects.requireNonNullElse(description, "");
        author = Objects.requireNonNullElse(author, "unknown");
        files = List.copyOf(Objects.requireNonNullElse(files, List.of()));
        if (number <= 0) {
            throw new IllegalArgumentException("number must be positive");
        }
        if (additions < 0 || deletions < 0 || changedFiles < 0) {
            throw new IllegalArgumentException("change counts cannot be negative");
        }
    }

    public int totalChangedLines() {
        return additions + deletions;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
