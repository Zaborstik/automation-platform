package com.zaborstik.platform.core.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Действие платформы (system.action).
 * Связано с action_type; применимость к entity_type задаётся отдельно (action_applicable_entity_type).
 */
public record Action(
    String id,
    String displayName,
    String internalName,
    String metaValue,
    String description,
    String actionTypeId,
    Instant createdAt,
    Instant updatedAt
) {
    public Action(String id, String displayName, String internalName, String metaValue,
                 String description, String actionTypeId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.internalName = Objects.requireNonNull(internalName, "internalName cannot be null");
        this.metaValue = metaValue;
        this.description = description;
        this.actionTypeId = Objects.requireNonNull(actionTypeId, "actionTypeId cannot be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Короткий конструктор без времени (для in-memory). */
    public static Action of(String id, String displayName, String internalName, String description, String actionTypeId) {
        Instant now = Instant.now();
        return new Action(id, displayName, internalName, null, description, actionTypeId, now, now);
    }
}
