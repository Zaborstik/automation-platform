package com.zaborstik.platform.agent.dto;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Retry policy for transient agent execution errors.
 */
public record RetryPolicy(int maxRetries, long delayMs, List<String> retryableErrorPatterns) {

    public RetryPolicy {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be >= 0");
        }
        retryableErrorPatterns = retryableErrorPatterns == null
            ? List.of()
            : retryableErrorPatterns.stream()
                .filter(Objects::nonNull)
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .toList();
    }

    public boolean isRetryable(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank() || retryableErrorPatterns.isEmpty()) {
            return false;
        }
        String normalizedError = errorMessage.toLowerCase(Locale.ROOT);
        return retryableErrorPatterns.stream().anyMatch(normalizedError::contains);
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(2, 1000, List.of("timeout", "not found", "not visible"));
    }

    public static RetryPolicy noRetry() {
        return new RetryPolicy(0, 0, List.of());
    }
}
