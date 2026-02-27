package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

/**
 * Действия платформы. Схема system.
 */
@Entity
@Table(name = "action", schema = "system")
public class ActionEntity {

    @Id
    @Column(name = "shortname", nullable = false, length = 36)
    private String shortname;

    @Column(name = "displayname", nullable = false, length = 255)
    private String displayname;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @ElementCollection
    @CollectionTable(name = "action_applicable_entity_type", schema = "system", joinColumns = @JoinColumn(name = "action"))
    @Column(name = "entity_type")
    private Set<String> applicableEntityTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "action_metadata", schema = "system", joinColumns = @JoinColumn(name = "action"))
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

    public ActionEntity() {
    }

    public String getId() { return shortname; }
    public void setId(String id) { this.shortname = id; }
    public String getShortname() { return shortname; }
    public void setShortname(String shortname) { this.shortname = shortname; }
    public String getName() { return displayname; }
    public void setName(String name) { this.displayname = name; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public Set<String> getApplicableEntityTypes() { return applicableEntityTypes; }
    public void setApplicableEntityTypes(Set<String> applicableEntityTypes) { this.applicableEntityTypes = applicableEntityTypes != null ? new HashSet<>(applicableEntityTypes) : new HashSet<>(); }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>(); }
}
