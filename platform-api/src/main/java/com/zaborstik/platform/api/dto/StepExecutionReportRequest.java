package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * Payload pushed by the local executor after running a single plan step.
 *
 * <p>The server uses it to update the lifecycle state of the step
 * ({@code in_progress -> completed | failed}) and to persist a
 * {@code plan_step_log} entry tied to the active {@link com.zaborstik.platform.api.entity.PlanResultEntity}.
 */
public class StepExecutionReportRequest {

    @NotNull
    private Boolean success;
    private String actionId;
    private String message;
    private String error;
    private String screenshotPath;
    private Long executionTimeMs;
    private Instant executedAt;
    private Map<String, Object> metadata;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
