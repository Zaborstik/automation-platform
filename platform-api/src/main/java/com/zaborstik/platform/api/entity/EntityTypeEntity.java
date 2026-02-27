package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Типы сущностей (справочник). Схема zbrtstk — бизнес-логика.
 */
@Entity
@Table(name = "entity_type", schema = "zbrtstk")
public class EntityTypeEntity {

    @Id
    @Column(name = "shortname", nullable = false, length = 36)
    private String shortname;

    @Column(name = "displayname", nullable = false, length = 255)
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

    @ElementCollection
    @CollectionTable(name = "entity_type_metadata", schema = "zbrtstk", joinColumns = @JoinColumn(name = "entity_type"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

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

    public EntityTypeEntity() {
    }

    public String getId() { return shortname; }
    public void setId(String id) { this.shortname = id; }
    public String getShortname() { return shortname; }
    public void setShortname(String shortname) { this.shortname = shortname; }
    public String getName() { return displayname; }
    public void setName(String name) { this.displayname = name; }
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
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>(); }
}
