package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Запрос на регистрацию результата выполнения плана.
 */
public class CreatePlanResultRequest {

    @NotNull
    private Boolean success;
    @NotNull
    private Instant startedTime;
    @NotNull
    private Instant finishedTime;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Instant getStartedTime() { return startedTime; }
    public void setStartedTime(Instant startedTime) { this.startedTime = startedTime; }
    public Instant getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Instant finishedTime) { this.finishedTime = finishedTime; }
}
