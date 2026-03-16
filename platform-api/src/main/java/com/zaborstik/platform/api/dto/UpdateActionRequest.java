package com.zaborstik.platform.api.dto;

public class UpdateActionRequest {

    private String displayname;
    private String description;
    private String metaValue;

    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMetaValue() { return metaValue; }
    public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
}
