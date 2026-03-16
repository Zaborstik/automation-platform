package com.zaborstik.platform.api.dto;

public class WorkflowStepResponse {

    private String id;
    private String internalname;
    private String displayname;
    private Integer sortorder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public Integer getSortorder() { return sortorder; }
    public void setSortorder(Integer sortorder) { this.sortorder = sortorder; }
}
