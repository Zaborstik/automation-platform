package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

/**
 * Шаг ЖЦ (system.workflow_step).
 */
@Entity
@Table(name = "workflow_step", schema = "system")
public class WorkflowStepEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "internalname", nullable = false)
    private String internalname;

    @Column(name = "displayname", nullable = false)
    private String displayname;

    @Column(name = "sortorder")
    private Integer sortorder;

    public WorkflowStepEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public Integer getSortorder() { return sortorder; }
    public void setSortorder(Integer sortorder) { this.sortorder = sortorder; }
}
