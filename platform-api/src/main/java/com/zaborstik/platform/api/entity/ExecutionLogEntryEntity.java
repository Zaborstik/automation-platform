package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

/**
 * JPA Entity для ExecutionLogEntry.
 * Хранит записи лога выполнения планов.
 * 
 * JPA Entity for ExecutionLogEntry.
 * Stores plan execution log entries.
 */
@Entity
@Table(name = "execution_log_entries")
public class ExecutionLogEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_result_id", nullable = false)
    private ExecutionResultEntity executionResult;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;

    @Column(name = "step_target", length = 1000)
    private String stepTarget;

    @Column(name = "step_explanation", length = 2000)
    private String stepExplanation;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "executed_at", nullable = false)
    private java.time.Instant executedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "screenshot_path", length = 1000)
    private String screenshotPath;

    @Column(name = "logged_at", nullable = false)
    private java.time.Instant loggedAt;

    @PrePersist
    protected void onCreate() {
        if (loggedAt == null) {
            loggedAt = java.time.Instant.now();
        }
    }

    // Constructors
    public ExecutionLogEntryEntity() {
    }

    public ExecutionLogEntryEntity(String planId, Integer stepIndex, String stepType, 
                                   String stepTarget, String stepExplanation, Boolean success,
                                   String message, String error, java.time.Instant executedAt,
                                   Long executionTimeMs, String screenshotPath) {
        this.planId = planId;
        this.stepIndex = stepIndex;
        this.stepType = stepType;
        this.stepTarget = stepTarget;
        this.stepExplanation = stepExplanation;
        this.success = success;
        this.message = message;
        this.error = error;
        this.executedAt = executedAt;
        this.executionTimeMs = executionTimeMs;
        this.screenshotPath = screenshotPath;
        this.loggedAt = java.time.Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExecutionResultEntity getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(ExecutionResultEntity executionResult) {
        this.executionResult = executionResult;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public String getStepTarget() {
        return stepTarget;
    }

    public void setStepTarget(String stepTarget) {
        this.stepTarget = stepTarget;
    }

    public String getStepExplanation() {
        return stepExplanation;
    }

    public void setStepExplanation(String stepExplanation) {
        this.stepExplanation = stepExplanation;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public java.time.Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(java.time.Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public java.time.Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(java.time.Instant loggedAt) {
        this.loggedAt = loggedAt;
    }
}
