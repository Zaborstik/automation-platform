package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Payload sent by the local executor to close out a plan run.
 */
public class FinishPlanRunRequest {

    @NotNull
    private String planResultId;
    @NotNull
    private Boolean success;
    private int totalSteps;
    private int failedSteps;
    private Instant startedTime;
    private Instant finishedTime;

    public String getPlanResultId() { return planResultId; }
    public void setPlanResultId(String planResultId) { this.planResultId = planResultId; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public int getFailedSteps() { return failedSteps; }
    public void setFailedSteps(int failedSteps) { this.failedSteps = failedSteps; }
    public Instant getStartedTime() { return startedTime; }
    public void setStartedTime(Instant startedTime) { this.startedTime = startedTime; }
    public Instant getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Instant finishedTime) { this.finishedTime = finishedTime; }
}
