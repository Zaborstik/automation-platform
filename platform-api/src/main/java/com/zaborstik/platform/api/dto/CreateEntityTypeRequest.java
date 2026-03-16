package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateEntityTypeRequest {

    @NotBlank
    private String displayname;
    private String uiDescription;
    private String kmArticle;

    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getUiDescription() { return uiDescription; }
    public void setUiDescription(String uiDescription) { this.uiDescription = uiDescription; }
    public String getKmArticle() { return kmArticle; }
    public void setKmArticle(String kmArticle) { this.kmArticle = kmArticle; }
}
