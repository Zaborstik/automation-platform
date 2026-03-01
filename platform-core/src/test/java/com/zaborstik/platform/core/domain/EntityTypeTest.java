package com.zaborstik.platform.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EntityTypeTest {

    @Test
    void shouldCreateEntityTypeWithOf() {
        EntityType entityType = EntityType.of("ent-button", "Кнопка");
        assertEquals("ent-button", entityType.id());
        assertEquals("Кнопка", entityType.displayName());
        assertNotNull(entityType.createdAt());
        assertNotNull(entityType.updatedAt());
    }

    @Test
    void shouldCreateEntityTypeWithAllFields() {
        Instant now = Instant.now();
        EntityType entityType = new EntityType(
            "ent-page",
            "Страница",
            now,
            now,
            "km-1",
            "Контейнер экрана",
            null,
            null
        );
        assertEquals("ent-page", entityType.id());
        assertEquals("Страница", entityType.displayName());
        assertEquals(now, entityType.createdAt());
        assertEquals("km-1", entityType.kmArticle());
        assertEquals("Контейнер экрана", entityType.uiDescription());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () ->
            new EntityType(null, "Здание", now, now, null, null, null, null)
        );
    }

    @Test
    void shouldThrowExceptionWhenDisplayNameIsNull() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () ->
            new EntityType("ent-1", null, now, now, null, null, null, null)
        );
    }

    @Test
    void shouldSupportNullOptionalFields() {
        EntityType entityType = new EntityType("ent-1", "Форма", null, null, null, null, null, null);
        assertEquals("ent-1", entityType.id());
        assertNull(entityType.createdAt());
        assertNull(entityType.kmArticle());
    }
}
