package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Типы сущностей (system.entity_type).
 */
@Entity
@Table(name = "entity_type", schema = "system")
public class EntityTypeEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "displayname", nullable = false)
    private String displayname;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @Column(name = "km_article", length = 36)
    private String kmArticle;

    @Column(name = "ui_description", columnDefinition = "TEXT")
    private String uiDescription;

    @Column(name = "entityfieldlist", length = 10000)
    private String entityfieldlist;

    @Column(name = "buttons", length = 10000)
    private String buttons;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdTime == null) createdTime = now;
        if (updatedTime == null) updatedTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }

    public EntityTypeEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public String getKmArticle() { return kmArticle; }
    public void setKmArticle(String kmArticle) { this.kmArticle = kmArticle; }
    public String getUiDescription() { return uiDescription; }
    public void setUiDescription(String uiDescription) { this.uiDescription = uiDescription; }
    public String getEntityfieldlist() { return entityfieldlist; }
    public void setEntityfieldlist(String entityfieldlist) { this.entityfieldlist = entityfieldlist; }
    public String getButtons() { return buttons; }
    public void setButtons(String buttons) { this.buttons = buttons; }
}
