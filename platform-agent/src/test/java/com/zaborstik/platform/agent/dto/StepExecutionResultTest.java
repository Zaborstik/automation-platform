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

        assertTrue(result.success());
        assertEquals(1, result.retryCount());
        assertEquals(2, result.stepIndex());
        assertEquals("CLICK", result.commandType());
    }

    @Test
    void shouldKeepBackwardCompatibilityForOldFactories() {
        StepExecutionResult success = StepExecutionResult.success("wait", "result", "Done", 10, null);
        StepExecutionResult failure = StepExecutionResult.failure("wait", "result", "Err", 11);

        assertEquals(0, success.retryCount());
        assertEquals(-1, success.stepIndex());
        assertNull(success.commandType());

        assertEquals(0, failure.retryCount());
        assertEquals(-1, failure.stepIndex());
        assertNull(failure.commandType());
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

        assertEquals(2, result.retryCount());
        assertEquals(4, result.stepIndex());
        assertEquals("TYPE", result.commandType());
    }
}
