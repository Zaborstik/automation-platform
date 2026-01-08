package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;
import java.util.*;

/**
 * JPA Entity для Action.
 * Хранит метаданные о действиях в системе.
 * 
 * JPA Entity for Action.
 * Stores metadata about actions in the system.
 */
@Entity
@Table(name = "actions")
public class ActionEntity {
    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "action_applicable_entity_types", joinColumns = @JoinColumn(name = "action_id"))
    @Column(name = "entity_type_id")
    private Set<String> applicableEntityTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "action_metadata", joinColumns = @JoinColumn(name = "action_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.Instant.now();
        updatedAt = java.time.Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.Instant.now();
    }

    // Constructors
    public ActionEntity() {
    }

    public ActionEntity(String id, String name, String description, 
                       Set<String> applicableEntityTypes, Map<String, String> metadata) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.applicableEntityTypes = applicableEntityTypes != null 
            ? new HashSet<>(applicableEntityTypes) 
            : new HashSet<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getApplicableEntityTypes() {
        return applicableEntityTypes;
    }

    public void setApplicableEntityTypes(Set<String> applicableEntityTypes) {
        this.applicableEntityTypes = applicableEntityTypes != null 
            ? new HashSet<>(applicableEntityTypes) 
            : new HashSet<>();
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public java.time.Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.Instant createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
