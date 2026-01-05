package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanExecutionResultTest {

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
        assertEquals(planId, result.getPlanId());
        assertTrue(result.isSuccess());
        assertEquals(startedAt, result.getStartedAt());
        assertEquals(finishedAt, result.getFinishedAt());
        assertEquals(3, result.getLogEntries().size());
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
        assertFalse(result.isSuccess());
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
        assertNotNull(result.getLogEntries());
        assertTrue(result.getLogEntries().isEmpty());
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
            result.getLogEntries().add(createLogEntry("plan-1", 1, true));
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
        assertTrue(stepResults.stream().allMatch(StepExecutionResult::isSuccess));
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
        assertTrue(stepResults.stream().noneMatch(StepExecutionResult::isSuccess));
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
        assertTrue(result.getLogEntries().isEmpty());
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
            result.getStartedAt(),
            result.getFinishedAt()
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
        assertTrue(stepResults.get(0).isSuccess());
        assertFalse(stepResults.get(1).isSuccess());
        assertTrue(stepResults.get(2).isSuccess());
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
        PlanStep step = PlanStep.explain("Step " + stepIndex);
        StepExecutionResult result = success
            ? StepExecutionResult.success("explain", null, "OK", 10L, null)
            : StepExecutionResult.failure("explain", null, "Error", 10L);
        return new ExecutionLogEntry(planId, stepIndex, step, result, Instant.now());
    }
}

