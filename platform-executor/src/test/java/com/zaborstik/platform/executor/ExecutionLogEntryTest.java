package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionLogEntryTest {

    /** Создаёт шаг плана для тестов (новый API: record PlanStep). */
    private static PlanStep testStep(String id, String displayName) {
        return new PlanStep(
            id,
            "plan-1",
            "workflow-1",
            "in_progress",
            "entity-type-1",
            "entity-1",
            0,
            displayName,
            List.of()
        );
    }

    @Test
    void shouldCreateExecutionLogEntryWithAllFields() {
        // Given
        String planId = "plan-123";
        int stepIndex = 0;
        PlanStep step = testStep("step-1", "Test step");
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
        assertEquals(planId, entry.planId());
        assertEquals(stepIndex, entry.stepIndex());
        assertEquals(step, entry.step());
        assertEquals(result, entry.result());
        assertEquals(loggedAt, entry.loggedAt());
    }

    @Test
    void shouldUseCurrentTimeWhenLoggedAtIsNull() {
        // Given
        PlanStep step = testStep("step-1", "Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);
        Instant before = Instant.now();

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", 0, step, result, null);
        Instant after = Instant.now();

        // Then
        assertNotNull(entry.loggedAt());
        assertTrue(entry.loggedAt().isAfter(before.minusSeconds(1)));
        assertTrue(entry.loggedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void shouldThrowExceptionWhenPlanIdIsNull() {
        // Given
        PlanStep step = testStep("step-1", "Test");
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
        PlanStep step = testStep("step-1", "Test");

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new ExecutionLogEntry("plan-1", 0, step, null, Instant.now());
        });
    }

    @Test
    void shouldSupportNegativeStepIndex() {
        // Given
        PlanStep step = testStep("step-1", "Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", -1, step, result, Instant.now());

        // Then
        assertEquals(-1, entry.stepIndex());
    }

    @Test
    void shouldSupportLargeStepIndex() {
        // Given
        PlanStep step = testStep("step-1", "Test");
        StepExecutionResult result = StepExecutionResult.success("explain", null, "OK", 10L, null);

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", Integer.MAX_VALUE, step, result, Instant.now());

        // Then
        assertEquals(Integer.MAX_VALUE, entry.stepIndex());
    }

    @Test
    void shouldSupportFailureResult() {
        // Given
        PlanStep step = testStep("step-1", "Click");
        StepExecutionResult failureResult = StepExecutionResult.failure(
            "click",
            "button",
            "Element not found",
            50L
        );

        // When
        ExecutionLogEntry entry = new ExecutionLogEntry("plan-1", 0, step, failureResult, Instant.now());

        // Then
        assertFalse(entry.result().success());
        assertEquals("Element not found", entry.result().error());
    }

    @Test
    void shouldReturnCorrectToString() {
        // Given
        PlanStep step = testStep("step-1", "Test step");
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

