package com.zaborstik.platform.api.dto;

public class WorkflowResponse {

    private String id;
    private String displayname;
    private String firstStepId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getFirstStepId() { return firstStepId; }
    public void setFirstStepId(String firstStepId) { this.firstStepId = firstStepId; }
}
