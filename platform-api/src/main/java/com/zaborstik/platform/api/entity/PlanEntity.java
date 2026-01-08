package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity для Plan.
 * Хранит планы выполнения действий.
 * 
 * JPA Entity for Plan.
 * Stores action execution plans.
 */
@Entity
@Table(name = "plans")
public class PlanEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "entity_type_id", nullable = false)
    private String entityTypeId;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "action_id", nullable = false)
    private String actionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlanStatus status;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepIndex ASC")
    private List<PlanStepEntity> steps = new ArrayList<>();

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    public enum PlanStatus {
        CREATED,
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
        updatedAt = java.time.Instant.now();
        if (status == null) {
            status = PlanStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.Instant.now();
    }

    // Constructors
    public PlanEntity() {
    }

    public PlanEntity(String id, String entityTypeId, String entityId, String actionId, PlanStatus status) {
        this.id = id;
        this.entityTypeId = entityTypeId;
        this.entityId = entityId;
        this.actionId = actionId;
        this.status = status != null ? status : PlanStatus.CREATED;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public List<PlanStepEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStepEntity> steps) {
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();
        // Устанавливаем обратную связь
        if (this.steps != null) {
            for (PlanStepEntity step : this.steps) {
                step.setPlan(this);
            }
        }
    }

    public void addStep(PlanStepEntity step) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(step);
        step.setPlan(this);
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
