package com.zaborstik.platform.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_transition", schema = "system")
public class WorkflowTransitionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow", nullable = false)
    private WorkflowEntity workflow;

    @Column(name = "from_step", nullable = false, length = 255)
    private String fromStep;

    @Column(name = "to_step", nullable = false, length = 255)
    private String toStep;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public WorkflowEntity getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowEntity workflow) { this.workflow = workflow; }
    public String getFromStep() { return fromStep; }
    public void setFromStep(String fromStep) { this.fromStep = fromStep; }
    public String getToStep() { return toStep; }
    public void setToStep(String toStep) { this.toStep = toStep; }
}
