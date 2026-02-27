package com.zaborstik.platform.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Привязка действия к UI (селектор). Схема system.
 */
@Entity
@Table(name = "ui_binding", schema = "system")
public class UIBindingEntity {

    @Id
    @Column(name = "action", nullable = false, length = 36)
    private String action;

    @Column(name = "selector", nullable = false, length = 1000)
    private String selector;

    @Enumerated(EnumType.STRING)
    @Column(name = "selector_type", nullable = false, length = 50)
    private SelectorType selectorType;

    @Column(name = "created_time", nullable = false)
    private Instant createdTime;

    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @ElementCollection
    @CollectionTable(name = "ui_binding_metadata", schema = "system", joinColumns = @JoinColumn(name = "ui_binding", referencedColumnName = "action"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", columnDefinition = "TEXT")
    private Map<String, String> metadata = new HashMap<>();

    public enum SelectorType { CSS, XPATH, TEXT, ACTION_ID }

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

    public UIBindingEntity() {
    }

    public String getActionId() { return action; }
    public void setActionId(String actionId) { this.action = actionId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }
    public SelectorType getSelectorType() { return selectorType; }
    public void setSelectorType(SelectorType selectorType) { this.selectorType = selectorType; }
    public Instant getCreatedTime() { return createdTime; }
    public void setCreatedTime(Instant createdTime) { this.createdTime = createdTime; }
    public Instant getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Instant updatedTime) { this.updatedTime = updatedTime; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>(); }
}
