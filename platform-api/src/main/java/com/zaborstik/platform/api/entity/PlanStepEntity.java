package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Шаг плана. Схема system.
 */
@Entity
@Table(name = "plan_step", schema = "system")
public class PlanStepEntity {

    @Id
    @Column(name = "shortname", nullable = false, length = 36)
    private String shortname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan", nullable = false)
    private PlanEntity plan;

    @Column(name = "workflow", length = 36)
    private String workflow;

    @Column(name = "workflowstepname", length = 255)
    private String workflowstepname;

    @Column(name = "entitytype", length = 36)
    private String entitytype;

    @Column(name = "entity_shortname", length = 36)
    private String entityShortname;

    @Column(name = "sortorder", nullable = false)
    private int sortorder;

    @Column(name = "step_type", length = 36)
    private String stepType;

    @Column(name = "target", length = 1000)
    private String target;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @Column(name = "displayname", length = 255)
    private String displayname;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @ElementCollection
    @CollectionTable(name = "plan_step_parameter", schema = "system", joinColumns = @JoinColumn(name = "plan_step"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, String> parameters = new HashMap<>();

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

    public PlanStepEntity() {
    }

    public String getShortname() { return shortname; }
    public void setShortname(String shortname) { this.shortname = shortname; }
    public PlanEntity getPlan() { return plan; }
    public void setPlan(PlanEntity plan) { this.plan = plan; }
    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }
    public String getWorkflowstepname() { return workflowstepname; }
    public void setWorkflowstepname(String workflowstepname) { this.workflowstepname = workflowstepname; }
    public String getEntitytype() { return entitytype; }
    public void setEntitytype(String entitytype) { this.entitytype = entitytype; }
    public String getEntityShortname() { return entityShortname; }
    public void setEntityShortname(String entityShortname) { this.entityShortname = entityShortname; }
    public int getSortorder() { return sortorder; }
    public void setSortorder(int sortorder) { this.sortorder = sortorder; }
    public int getStepIndex() { return sortorder; }
    public void setStepIndex(int stepIndex) { this.sortorder = stepIndex; }
    public String getType() { return stepType; }
    public void setType(String stepType) { this.stepType = stepType; }
    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }

    /** Параметры шага (meta_key -> meta_value). */
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>(); }
}
