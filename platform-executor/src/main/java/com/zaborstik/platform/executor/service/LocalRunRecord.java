package com.zaborstik.platform.executor.service;

import java.time.Instant;

/**
 * In-memory snapshot of a single local plan run that the {@code /local}
 * REST endpoints expose to callers (chat overlay, CLI, smoke tests).
 *
 * <p>Authoritative persistent state still lives on {@code platform-api}; this
 * record only mirrors enough to drive the UI on the user's machine.
 */
public class LocalRunRecord {

    private final String runId;
    private volatile String planId;
    private volatile String planResultId;
    private volatile LocalRunStatus status;
    private volatile int totalSteps;
    private volatile int failedSteps;
    private volatile String message;
    private final Instant startedAt;
    private volatile Instant finishedAt;

    public LocalRunRecord(String runId) {
        this.runId = runId;
        this.status = LocalRunStatus.PENDING;
        this.startedAt = Instant.now();
    }

    public String getRunId() { return runId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getPlanResultId() { return planResultId; }
    public void setPlanResultId(String planResultId) { this.planResultId = planResultId; }
    public LocalRunStatus getStatus() { return status; }
    public void setStatus(LocalRunStatus status) { this.status = status; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public int getFailedSteps() { return failedSteps; }
    public void setFailedSteps(int failedSteps) { this.failedSteps = failedSteps; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
