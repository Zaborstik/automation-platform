package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Entity для UIBinding.
 * Хранит привязки действий к UI-элементам.
 */
@Entity
@Table(name = "ui_bindings")
public class UIBindingEntity {
    @Id
    @Column(name = "action_id", nullable = false, unique = true)
    private String actionId;

    @Column(name = "selector", nullable = false, length = 1000)
    private String selector;

    @Enumerated(EnumType.STRING)
    @Column(name = "selector_type", nullable = false)
    private SelectorType selectorType;

    @ElementCollection
    @CollectionTable(name = "ui_binding_metadata", joinColumns = @JoinColumn(name = "action_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    public enum SelectorType {
        CSS,
        XPATH,
        TEXT,
        ACTION_ID
    }

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
    public UIBindingEntity() {
    }

    public UIBindingEntity(String actionId, String selector, SelectorType selectorType, 
                          Map<String, String> metadata) {
        this.actionId = actionId;
        this.selector = selector;
        this.selectorType = selectorType;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters and Setters
    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public SelectorType getSelectorType() {
        return selectorType;
    }

    public void setSelectorType(SelectorType selectorType) {
        this.selectorType = selectorType;
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
