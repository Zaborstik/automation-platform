package com.zaborstik.platform.core.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Тип сущности (zbrtstk.entity_type).
 * Типы объектов дочерней системы: страница, форма, кнопка и т.д.
 */
public record EntityType(
    String id,
    String displayName,
    Instant createdAt,
    Instant updatedAt,
    String kmArticle,
    String uiDescription,
    String entityFieldList,
    String buttons
) {
    public EntityType(String id, String displayName, Instant createdAt, Instant updatedAt,
                      String kmArticle, String uiDescription, String entityFieldList, String buttons) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.kmArticle = kmArticle;
        this.uiDescription = uiDescription;
        this.entityFieldList = entityFieldList;
        this.buttons = buttons;
    }

    /** Короткий конструктор для тестов/памяти без времени. */
    public static EntityType of(String id, String displayName) {
        Instant now = Instant.now();
        return new EntityType(id, displayName, now, now, null, null, null, null);
    }
}
