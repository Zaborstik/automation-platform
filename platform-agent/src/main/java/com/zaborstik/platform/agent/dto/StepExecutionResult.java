package com.zaborstik.platform.agent.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * Результат выполнения шага плана.
 */
public class StepExecutionResult {
    private final String stepType;
    private final String stepTarget;
    private final boolean success;
    private final String message;
    private final String error;
    private final Instant executedAt;
    private final long executionTimeMs;
    private final String screenshotPath;

    public StepExecutionResult(String stepType, String stepTarget, boolean success, 
                               String message, String error, Instant executedAt, 
                               long executionTimeMs, String screenshotPath) {
        this.stepType = Objects.requireNonNull(stepType, "Step type cannot be null");
        this.stepTarget = stepTarget;
        this.success = success;
        this.message = message;
        this.error = error;
        this.executedAt = executedAt != null ? executedAt : Instant.now();
        this.executionTimeMs = executionTimeMs;
        this.screenshotPath = screenshotPath;
    }

    public String getStepType() {
        return stepType;
    }

    public String getStepTarget() {
        return stepTarget;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public static StepExecutionResult success(String stepType, String stepTarget, 
                                              String message, long executionTimeMs, 
                                              String screenshotPath) {
        return new StepExecutionResult(stepType, stepTarget, true, message, null, 
                                      Instant.now(), executionTimeMs, screenshotPath);
    }

    public static StepExecutionResult failure(String stepType, String stepTarget, 
                                              String error, long executionTimeMs) {
        return new StepExecutionResult(stepType, stepTarget, false, null, error, 
                                      Instant.now(), executionTimeMs, null);
    }

    @Override
    public String toString() {
        return "StepExecutionResult{type='" + stepType + "', target='" + stepTarget + 
               "', success=" + success + ", message='" + message + 
               "', error='" + error + "', time=" + executionTimeMs + "ms}";
    }
}

