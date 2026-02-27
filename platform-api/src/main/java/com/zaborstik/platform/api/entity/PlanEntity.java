package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * План выполнения. Схема system.
 */
@Entity
@Table(name = "plan", schema = "system")
public class PlanEntity {

    @Id
    @Column(name = "shortname", nullable = false, length = 36)
    private String shortname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action", nullable = false)
    private ActionEntity action;

    @Column(name = "workflow", length = 36)
    private String workflow;

    @Column(name = "workflowstepname", length = 255)
    private String workflowstepname;

    @Column(name = "stopped_at_step", length = 255)
    private String stoppedAtStep;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @Column(name = "entity_type_id", length = 36)
    private String entityTypeId;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private PlanStatus status;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortorder")
    private List<PlanStepEntity> steps = new ArrayList<>();

    public enum PlanStatus { CREATED, EXECUTING, COMPLETED, FAILED, CANCELLED }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdTime == null) createdTime = now;
        if (updatedTime == null) updatedTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }

    public PlanEntity() {
    }

    public String getId() { return shortname; }
    public void setId(String id) { this.shortname = id; }
    public String getShortname() { return shortname; }
    public void setShortname(String shortname) { this.shortname = shortname; }
    public ActionEntity getAction() { return action; }
    public void setAction(ActionEntity action) { this.action = action; }
    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }
    public String getWorkflowstepname() { return workflowstepname; }
    public void setWorkflowstepname(String workflowstepname) { this.workflowstepname = workflowstepname; }
    public String getStoppedAtStep() { return stoppedAtStep; }
    public void setStoppedAtStep(String stoppedAtStep) { this.stoppedAtStep = stoppedAtStep; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public String getEntityTypeId() { return entityTypeId; }
    public void setEntityTypeId(String entityTypeId) { this.entityTypeId = entityTypeId; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
    public String getActionId() { return action != null ? action.getShortname() : null; }
    public List<PlanStepEntity> getSteps() { return steps; }
    public void setSteps(List<PlanStepEntity> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
        for (PlanStepEntity step : this.steps) step.setPlan(this);
    }
    public void addStep(PlanStepEntity step) {
        step.setPlan(this);
        this.steps.add(step);
    }
}
