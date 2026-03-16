package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateActionRequest {

    @NotBlank
    private String displayname;
    @NotBlank
    private String internalname;
    private String description;
    @NotBlank
    private String actionTypeId;
    private String metaValue;

    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getActionTypeId() { return actionTypeId; }
    public void setActionTypeId(String actionTypeId) { this.actionTypeId = actionTypeId; }
    public String getMetaValue() { return metaValue; }
    public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
}
