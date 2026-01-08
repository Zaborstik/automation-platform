package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity для ExecutionResult.
 * Хранит результаты выполнения планов.
 */
@Entity
@Table(name = "execution_results")
public class ExecutionResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false, unique = true)
    private String planId;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "started_at", nullable = false)
    private java.time.Instant startedAt;

    @Column(name = "finished_at")
    private java.time.Instant finishedAt;

    @OneToMany(mappedBy = "executionResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepIndex ASC")
    private List<ExecutionLogEntryEntity> logEntries = new ArrayList<>();

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
    }

    // Constructors
    public ExecutionResultEntity() {
    }

    public ExecutionResultEntity(String planId, Boolean success, java.time.Instant startedAt, 
                                java.time.Instant finishedAt) {
        this.planId = planId;
        this.success = success;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public java.time.Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(java.time.Instant startedAt) {
        this.startedAt = startedAt;
    }

    public java.time.Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(java.time.Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<ExecutionLogEntryEntity> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<ExecutionLogEntryEntity> logEntries) {
        this.logEntries = logEntries != null ? new ArrayList<>(logEntries) : new ArrayList<>();
        // Устанавливаем обратную связь
        if (this.logEntries != null) {
            for (ExecutionLogEntryEntity entry : this.logEntries) {
                entry.setExecutionResult(this);
            }
        }
    }

    public void addLogEntry(ExecutionLogEntryEntity entry) {
        if (logEntries == null) {
            logEntries = new ArrayList<>();
        }
        logEntries.add(entry);
        entry.setExecutionResult(this);
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }
}
