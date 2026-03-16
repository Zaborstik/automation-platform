package com.zaborstik.platform.agent.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StepExecutionResultTest {

    @Test
    void shouldCreateResultWithExtendedFields() {
        StepExecutionResult result = StepExecutionResult.success(
            "click",
            "#submit",
            "OK",
            123,
            "/tmp/screen.png",
            Map.of("k", "v"),
            1,
            2,
            "CLICK"
        );

        assertTrue(result.isSuccess());
        assertEquals(1, result.getRetryCount());
        assertEquals(2, result.getStepIndex());
        assertEquals("CLICK", result.getCommandType());
    }

    @Test
    void shouldKeepBackwardCompatibilityForOldFactories() {
        StepExecutionResult success = StepExecutionResult.success("wait", "result", "Done", 10, null);
        StepExecutionResult failure = StepExecutionResult.failure("wait", "result", "Err", 11);

        assertEquals(0, success.getRetryCount());
        assertEquals(-1, success.getStepIndex());
        assertNull(success.getCommandType());

        assertEquals(0, failure.getRetryCount());
        assertEquals(-1, failure.getStepIndex());
        assertNull(failure.getCommandType());
    }

    @Test
    void gettersShouldExposeExtendedFields() {
        StepExecutionResult result = StepExecutionResult.failure(
            "type",
            "#q",
            "timeout",
            500,
            Map.of(),
            2,
            4,
            "TYPE"
        );

        assertEquals(2, result.getRetryCount());
        assertEquals(4, result.getStepIndex());
        assertEquals("TYPE", result.getCommandType());
    }
}
