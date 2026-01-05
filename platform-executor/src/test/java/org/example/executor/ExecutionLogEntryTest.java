package org.example.executor;

import org.example.agent.dto.StepExecutionResult;
import org.example.core.plan.PlanStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionLogEntryTest {

    @Test
    void shouldCreateExecutionLogEntryWithAllFields() {
        // Given
        String planId = "plan-123";
        int stepIndex = 0;
        PlanStep step = PlanStep.explain("Test step");
        StepExecutionResult result = StepExecutionResult.success(
            "explain",
            null,
            "Step executed",
            100L,
            null
        );
        Instant loggedAt = Instant.now();

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry(planId, stepIndex, step, result, loggedAt);

        // Then
        assertEquals(planId, entry.getPlanId());
        assertEquals(stepIndex, entry.getStepIndex());
        assertEquals(step, entry.getStep());
        assertEquals(result, entry.getResult());
        assertEquals(loggedAt, entry.getLoggedAt());
    }

    @Test
    void shouldUseCurrentTimeWhenLoggedAtIsNull() {
        // Given
        PlanStep step = PlanStep.explain("Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);
        Instant before = Instant.now();

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", 0, step, result, null);
        Instant after = Instant.now();

        // Then
        assertNotNull(entry.getLoggedAt());
        assertTrue(entry.getLoggedAt().isAfter(before.minusSeconds(1)));
        assertTrue(entry.getLoggedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void shouldThrowExceptionWhenPlanIdIsNull() {
        // Given
        PlanStep step = PlanStep.explain("Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new ExecutionLogEntry(null, 0, step, result, Instant.now());
        });
    }

    @Test
    void shouldThrowExceptionWhenStepIsNull() {
        // Given
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new ExecutionLogEntry("plan-1", 0, null, result, Instant.now());
        });
    }

    @Test
    void shouldThrowExceptionWhenResultIsNull() {
        // Given
        PlanStep step = PlanStep.explain("Test");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new ExecutionLogEntry("plan-1", 0, step, null, Instant.now());
        });
    }

    @Test
    void shouldSupportNegativeStepIndex() {
        // Given
        PlanStep step = PlanStep.explain("Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", -1, step, result, Instant.now());

        // Then
        assertEquals(-1, entry.getStepIndex());
    }

    @Test
    void shouldSupportLargeStepIndex() {
        // Given
        PlanStep step = PlanStep.explain("Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", Integer.MAX_VALUE, step, result, Instant.now());

        // Then
        assertEquals(Integer.MAX_VALUE, entry.getStepIndex());
    }

    @Test
    void shouldSupportFailureResult() {
        // Given
        PlanStep step = PlanStep.click("button", "Click");
        StepExecutionResult failureResult = StepExecutionResult.failure(
            "click",
            "button",
            "Element not found",
            50L
        );

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", 0, step, failureResult, Instant.now());

        // Then
        assertFalse(entry.getResult().isSuccess());
        assertEquals("Element not found", entry.getResult().getError());
    }

    @Test
    void shouldReturnCorrectToString() {
        // Given
        PlanStep step = PlanStep.explain("Test step");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-123", 5, step, result, Instant.now());

        // When
        String toString = entry.toString();

        // Then
        assertTrue(toString.contains("plan-123"));
        assertTrue(toString.contains("5"));
        assertTrue(toString.contains("ExecutionLogEntry"));
    }
}

