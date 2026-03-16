package com.zaborstik.platform.agent.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void defaultPolicyShouldContainExpectedValues() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertEquals(2, policy.maxRetries());
        assertEquals(1000, policy.delayMs());
        assertTrue(policy.retryableErrorPatterns().contains("timeout"));
        assertTrue(policy.retryableErrorPatterns().contains("not found"));
        assertTrue(policy.retryableErrorPatterns().contains("not visible"));
    }

    @Test
    void noRetryShouldDisableRetries() {
        RetryPolicy policy = RetryPolicy.noRetry();

        assertEquals(0, policy.maxRetries());
        assertEquals(0, policy.delayMs());
        assertTrue(policy.retryableErrorPatterns().isEmpty());
    }

    @Test
    void isRetryableShouldMatchKnownPattern() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertTrue(policy.isRetryable("Element not found on page"));
    }

    @Test
    void isRetryableShouldReturnFalseForUnknownError() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();

        assertFalse(policy.isRetryable("Something unexpected"));
    }
}
