package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

/**
 * Типы действия (system.action_type). Определяются для среды (RAD/LLM).
 */
@Entity
@Table(name = "action_type", schema = "system")
public class ActionTypeEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "internalname", nullable = false, length = 255)
    private String internalname;

    @Column(name = "displayname", nullable = false, length = 255)
    private String displayname;

    public ActionTypeEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
}
