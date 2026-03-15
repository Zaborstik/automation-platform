package com.zaborstik.platform.api.dto;

import java.time.Instant;

/**
 * Ответ на запуск выполнения плана.
 */
public class ExecutePlanResponse {
    private String planId;
    private String planResultId;
    private boolean success;
    private int totalSteps;
    private int failedSteps;
    private Instant startedTime;
    private Instant finishedTime;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanResultId() {
        return planResultId;
    }

    public void setPlanResultId(String planResultId) {
        this.planResultId = planResultId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public int getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(int failedSteps) {
        this.failedSteps = failedSteps;
    }

    public Instant getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(Instant startedTime) {
        this.startedTime = startedTime;
    }

    public Instant getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Instant finishedTime) {
        this.finishedTime = finishedTime;
    }
}
