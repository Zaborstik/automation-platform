package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * План выполнения (zbrtstk.plan). Задача пользователя; имеет ЖЦ и шаг, на котором остановилось выполнение.
 */
@Entity
@Table(name = "plan", schema = "zbrtstk")
public class PlanEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow", nullable = false)
    private WorkflowEntity workflow;

    @Column(name = "workflow_step_internalname", nullable = false, length = 255)
    private String workflowStepInternalname;

    @Column(name = "stopped_at_plan_step", nullable = false, length = 36)
    private String stoppedAtPlanStep;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time")
    private Instant updatedTime;

    @Column(name = "target", length = 510)
    private String target;

    @Column(name = "explanation", length = 1020)
    private String explanation;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortorder")
    private List<PlanStepEntity> steps = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) createdTime = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }

    public PlanEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public WorkflowEntity getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowEntity workflow) { this.workflow = workflow; }
    public String getWorkflowStepInternalname() { return workflowStepInternalname; }
    public void setWorkflowStepInternalname(String workflowStepInternalname) { this.workflowStepInternalname = workflowStepInternalname; }
    public String getStoppedAtPlanStep() { return stoppedAtPlanStep; }
    public void setStoppedAtPlanStep(String stoppedAtPlanStep) { this.stoppedAtPlanStep = stoppedAtPlanStep; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<PlanStepEntity> getSteps() { return steps; }
    public void setSteps(List<PlanStepEntity> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
        for (PlanStepEntity s : this.steps) s.setPlan(this);
    }
    public void addStep(PlanStepEntity step) {
        step.setPlan(this);
        this.steps.add(step);
    }
}
