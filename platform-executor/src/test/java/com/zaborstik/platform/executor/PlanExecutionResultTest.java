package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanExecutionResultTest {

    private static PlanStep testStep(String id, String displayName) {
        return new PlanStep(
            id,
            "plan-1",
            "wf-1",
            "in_progress",
            "ent-1",
            "e1",
            0,
            displayName,
            List.of()
        );
    }

    @Test
    void shouldCreatePlanExecutionResultWithAllFields() {
        // Given
        String planId = "plan-123";
        boolean success = true;
        Instant startedAt = Instant.now().minusSeconds(10);
        Instant finishedAt = Instant.now();
        List<ExecutionLogEntry> logEntries = createTestLogEntries(planId, 3, true);

        // When
        PlanExecutionResult result = new PlanExecutionResult(
            planId,
            success,
            startedAt,
            finishedAt,
            logEntries
        );

        // Then
        assertEquals(planId, result.planId());
        assertTrue(result.success());
        assertEquals(startedAt, result.startedAt());
        assertEquals(finishedAt, result.finishedAt());
        assertEquals(3, result.logEntries().size());
    }

    @Test
    void shouldCreateFailedResult() {
        // Given
        String planId = "plan-456";
        List<ExecutionLogEntry> logEntries = createTestLogEntries(planId, 2, false);

        // When
        PlanExecutionResult result = new PlanExecutionResult(
            planId,
            false,
            Instant.now().minusSeconds(5),
            Instant.now(),
            logEntries
        );

        // Then
        assertFalse(result.success());
    }

    @Test
    void shouldReturnEmptyListWhenLogEntriesIsNull() {
        // When
        PlanExecutionResult result = new PlanExecutionResult(
            "plan-1",
            true,
            Instant.now(),
            Instant.now(),
            null
        );

        // Then
        assertNotNull(result.logEntries());
        assertTrue(result.logEntries().isEmpty());
    }

    @Test
    void shouldReturnImmutableLogEntries() {
        // Given
        List<ExecutionLogEntry> originalEntries = new ArrayList<>();
        originalEntries.add(createLogEntry("plan-1", 0, true));

        PlanExecutionResult result = new PlanExecutionResult(
            "plan-1",
            true,
            Instant.now(),
            Instant.now(),
            originalEntries
        );

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> {
            result.logEntries().add(createLogEntry("plan-1", 1, true));
        });
    }

    @Test
    void shouldReturnStepResultsCorrectly() {
        // Given
        String planId = "plan-789";
        List<ExecutionLogEntry> logEntries = createTestLogEntries(planId, 3, true);

        PlanExecutionResult result = new PlanExecutionResult(
            planId,
            true,
            Instant.now(),
            Instant.now(),
            logEntries
        );

        // When
        List<StepExecutionResult> stepResults = result.getStepResults();

        // Then
        assertEquals(3, stepResults.size());
        assertTrue(stepResults.stream().allMatch(StepExecutionResult::success));
    }

    @Test
    void shouldReturnStepResultsWithFailures() {
        // Given
        String planId = "plan-fail";
        List<ExecutionLogEntry> logEntries = createTestLogEntries(planId, 2, false);

        PlanExecutionResult result = new PlanExecutionResult(
            planId,
            false,
            Instant.now(),
            Instant.now(),
            logEntries
        );

        // When
        List<StepExecutionResult> stepResults = result.getStepResults();

        // Then
        assertEquals(2, stepResults.size());
        assertTrue(stepResults.stream().noneMatch(StepExecutionResult::success));
    }

    @Test
    void shouldThrowExceptionWhenPlanIdIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new PlanExecutionResult(
                null,
                true,
                Instant.now(),
                Instant.now(),
                List.of()
            );
        });
    }

    @Test
    void shouldHandleEmptyLogEntries() {
        // When
        PlanExecutionResult result = new PlanExecutionResult(
            "plan-empty",
            true,
            Instant.now(),
            Instant.now(),
            List.of()
        );

        // Then
        assertTrue(result.logEntries().isEmpty());
        assertTrue(result.getStepResults().isEmpty());
    }

    @Test
    void shouldCalculateExecutionTimeCorrectly() {
        // Given
        Instant startedAt = Instant.now().minusSeconds(10);
        Instant finishedAt = Instant.now();

        PlanExecutionResult result = new PlanExecutionResult(
            "plan-time",
            true,
            startedAt,
            finishedAt,
            List.of()
        );

        // When
        long executionTimeMs = java.time.Duration.between(
            result.startedAt(),
            result.finishedAt()
        ).toMillis();

        // Then
        assertTrue(executionTimeMs >= 9000 && executionTimeMs <= 11000); // ~10 seconds
    }

    @Test
    void shouldReturnCorrectToString() {
        // Given
        List<ExecutionLogEntry> logEntries = createTestLogEntries("plan-str", 2, true);
        PlanExecutionResult result = new PlanExecutionResult(
            "plan-str",
            true,
            Instant.now(),
            Instant.now(),
            logEntries
        );

        // When
        String toString = result.toString();

        // Then
        assertTrue(toString.contains("plan-str"));
        assertTrue(toString.contains("true"));
        assertTrue(toString.contains("2"));
        assertTrue(toString.contains("PlanExecutionResult"));
    }

    @Test
    void shouldHandleMixedSuccessAndFailure() {
        // Given
        String planId = "plan-mixed";
        List<ExecutionLogEntry> logEntries = new ArrayList<>();
        logEntries.add(createLogEntry(planId, 0, true));
        logEntries.add(createLogEntry(planId, 1, false));
        logEntries.add(createLogEntry(planId, 2, true));

        PlanExecutionResult result = new PlanExecutionResult(
            planId,
            false, // overall failure because of step 1
            Instant.now(),
            Instant.now(),
            logEntries
        );

        // When
        List<StepExecutionResult> stepResults = result.getStepResults();

        // Then
        assertEquals(3, stepResults.size());
        assertTrue(stepResults.get(0).success());
        assertFalse(stepResults.get(1).success());
        assertTrue(stepResults.get(2).success());
    }

    // Helper methods

    private List<ExecutionLogEntry> createTestLogEntries(String planId, int count, boolean allSuccess) {
        List<ExecutionLogEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(createLogEntry(planId, i, allSuccess));
        }
        return entries;
    }

    private ExecutionLogEntry createLogEntry(String planId, int stepIndex, boolean success) {
        PlanStep step = testStep("step-" + stepIndex, "Step " + stepIndex);
        StepExecutionResult result = success
            ? StepExecutionResult.success("step", null, "OK", 10L, null)
            : StepExecutionResult.failure("step", null, "Error", 10L);
        return new ExecutionLogEntry(planId, stepIndex, step, result, Instant.now());
    }
}

