package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Шаг плана (zbrtstk.plan_step). Мини-задача: ЖЦ, тип сущности, объект действия; содержит plan_step_action.
 */
@Entity
@Table(name = "plan_step", schema = "zbrtstk")
public class PlanStepEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow", nullable = false)
    private WorkflowEntity workflow;

    @Column(name = "workflow_step_internalname", nullable = false, length = 255)
    private String workflowStepInternalname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entitytype", nullable = false)
    private EntityTypeEntity entitytype;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "sortorder", nullable = false)
    private int sortorder;

    @Column(name = "displayname", nullable = false, length = 255)
    private String displayname;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @OneToMany(mappedBy = "planStep", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanStepActionEntity> actions = new ArrayList<>();

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

    public PlanStepEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public PlanEntity getPlan() { return plan; }
    public void setPlan(PlanEntity plan) { this.plan = plan; }
    public WorkflowEntity getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowEntity workflow) { this.workflow = workflow; }
    public String getWorkflowStepInternalname() { return workflowStepInternalname; }
    public void setWorkflowStepInternalname(String workflowStepInternalname) { this.workflowStepInternalname = workflowStepInternalname; }
    public EntityTypeEntity getEntitytype() { return entitytype; }
    public void setEntitytype(EntityTypeEntity entitytype) { this.entitytype = entitytype; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public int getSortorder() { return sortorder; }
    public void setSortorder(int sortorder) { this.sortorder = sortorder; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public List<PlanStepActionEntity> getActions() { return actions; }
    public void setActions(List<PlanStepActionEntity> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
        for (PlanStepActionEntity a : this.actions) a.setPlanStep(this);
    }
}
