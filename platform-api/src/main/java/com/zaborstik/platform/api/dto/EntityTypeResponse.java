package com.zaborstik.platform.api.dto;

import java.time.Instant;

public class EntityTypeResponse {

    private String id;
    private String displayname;
    private String uiDescription;
    private String kmArticle;
    private String entityfieldlist;
    private String buttons;
    private Instant createdTime;
    private Instant updatedTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getUiDescription() { return uiDescription; }
    public void setUiDescription(String uiDescription) { this.uiDescription = uiDescription; }
    public String getKmArticle() { return kmArticle; }
    public void setKmArticle(String kmArticle) { this.kmArticle = kmArticle; }
    public String getEntityfieldlist() { return entityfieldlist; }
    public void setEntityfieldlist(String entityfieldlist) { this.entityfieldlist = entityfieldlist; }
    public String getButtons() { return buttons; }
    public void setButtons(String buttons) { this.buttons = buttons; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
}
