package com.zaborstik.platform.api.dto;

import java.time.Instant;

public class ActionResponse {

    private String id;
    private String displayname;
    private String internalname;
    private String description;
    private String metaValue;
    private String actionTypeId;
    private Instant createdTime;
    private Instant updatedTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMetaValue() { return metaValue; }
    public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
    public String getActionTypeId() { return actionTypeId; }
    public void setActionTypeId(String actionTypeId) { this.actionTypeId = actionTypeId; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
}
