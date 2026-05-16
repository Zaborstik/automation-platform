package com.zaborstik.platform.executor.dto;

import com.zaborstik.platform.executor.service.LocalRunRecord;
import com.zaborstik.platform.executor.service.LocalRunStatus;

import java.time.Instant;

/**
 * REST response shape exposed by {@code /local/run*} and {@code /local/status/*}.
 */
public class LocalRunResponse {

    private String runId;
    private String planId;
    private String planResultId;
    private LocalRunStatus status;
    private int totalSteps;
    private int failedSteps;
    private String message;
    private Instant startedAt;
    private Instant finishedAt;

    public static LocalRunResponse from(LocalRunRecord record) {
        LocalRunResponse response = new LocalRunResponse();
        response.runId = record.getRunId();
        response.planId = record.getPlanId();
        response.planResultId = record.getPlanResultId();
        response.status = record.getStatus();
        response.totalSteps = record.getTotalSteps();
        response.failedSteps = record.getFailedSteps();
        response.message = record.getMessage();
        response.startedAt = record.getStartedAt();
        response.finishedAt = record.getFinishedAt();
        return response;
    }

    public String getRunId() { return runId; }
    public String getPlanId() { return planId; }
    public String getPlanResultId() { return planResultId; }
    public LocalRunStatus getStatus() { return status; }
    public int getTotalSteps() { return totalSteps; }
    public int getFailedSteps() { return failedSteps; }
    public String getMessage() { return message; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
