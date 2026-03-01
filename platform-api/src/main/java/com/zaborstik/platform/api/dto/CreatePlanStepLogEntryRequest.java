package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Запрос на создание записи лога по шагу плана (при ошибке/прерывании).
 */
public class CreatePlanStepLogEntryRequest {

    @NotBlank
    private String planStepId;
    @NotBlank
    private String planResultId;
    @NotBlank
    private String actionId;
    @NotBlank
    private String message;
    private String error;
    @NotNull
    private Instant executedTime;
    private Long executionTimeMs;
    private String attachmentId;

    public String getPlanStepId() { return planStepId; }
    public void setPlanStepId(String planStepId) { this.planStepId = planStepId; }
    public String getPlanResultId() { return planResultId; }
    public void setPlanResultId(String planResultId) { this.planResultId = planResultId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getExecutedTime() { return executedTime; }
    public void setExecutedTime(Instant executedTime) { this.executedTime = executedTime; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }
}
