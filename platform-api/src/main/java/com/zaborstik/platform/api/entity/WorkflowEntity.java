package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

/**
 * ЖЦ (system.workflow). У плана и шага плана свой workflow.
 */
@Entity
@Table(name = "workflow", schema = "system")
public class WorkflowEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "displayname", nullable = false)
    private String displayname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firststep", nullable = false)
    private WorkflowStepEntity firststep;

    public WorkflowEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public WorkflowStepEntity getFirststep() { return firststep; }
    public void setFirststep(WorkflowStepEntity firststep) { this.firststep = firststep; }
}
