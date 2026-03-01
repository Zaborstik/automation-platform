package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Действия платформы (system.action). Генерируются на основе БЗ/RAD; применимость к entity_type — action_applicable_entity_type.
 */
@Entity
@Table(name = "action", schema = "system")
public class ActionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "displayname", nullable = false, length = 255)
    private String displayname;

    @Column(name = "internalname", nullable = false, length = 255)
    private String internalname;

    @Column(name = "meta_value", columnDefinition = "TEXT")
    private String metaValue;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_type", nullable = false)
    private ActionTypeEntity actionType;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @ManyToMany
    @JoinTable(
        name = "action_applicable_entity_type",
        schema = "system",
        joinColumns = @JoinColumn(name = "action"),
        inverseJoinColumns = @JoinColumn(name = "entity_type")
    )
    private Set<EntityTypeEntity> applicableEntityTypes = new HashSet<>();

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

    public ActionEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayname() { return displayname; }
    public void setDisplayname(String displayname) { this.displayname = displayname; }
    public String getInternalname() { return internalname; }
    public void setInternalname(String internalname) { this.internalname = internalname; }
    public String getMetaValue() { return metaValue; }
    public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ActionTypeEntity getActionType() { return actionType; }
    public void setActionType(ActionTypeEntity actionType) { this.actionType = actionType; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public Set<EntityTypeEntity> getApplicableEntityTypes() { return applicableEntityTypes; }
    public void setApplicableEntityTypes(Set<EntityTypeEntity> applicableEntityTypes) { this.applicableEntityTypes = applicableEntityTypes != null ? applicableEntityTypes : new HashSet<>(); }
}
