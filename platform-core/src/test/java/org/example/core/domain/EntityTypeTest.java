package org.example.core.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EntityTypeTest {

    @Test
    void shouldCreateEntityTypeWithAllFields() {
        Map<String, Object> metadata = Map.of("description", "Test entity");
        EntityType entityType = new EntityType("Building", "Здание", metadata);

        assertEquals("Building", entityType.getId());
        assertEquals("Здание", entityType.getName());
        assertEquals(metadata, entityType.getMetadata());
    }

    @Test
    void shouldCreateEntityTypeWithNullMetadata() {
        EntityType entityType = new EntityType("Building", "Здание", null);

        assertEquals("Building", entityType.getId());
        assertEquals("Здание", entityType.getName());
        assertTrue(entityType.getMetadata().isEmpty());
    }

    @Test
    void shouldCreateEntityTypeWithEmptyMetadata() {
        EntityType entityType = new EntityType("Building", "Здание", Map.of());

        assertEquals("Building", entityType.getId());
        assertTrue(entityType.getMetadata().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new EntityType(null, "Здание", Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new EntityType("Building", null, Map.of());
        });
    }

    @Test
    void shouldReturnImmutableMetadata() {
        Map<String, Object> originalMetadata = Map.of("key", "value");
        EntityType entityType = new EntityType("Building", "Здание", originalMetadata);

        Map<String, Object> returnedMetadata = entityType.getMetadata();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedMetadata.put("newKey", "newValue");
        });
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        EntityType type1 = new EntityType("Building", "Здание", Map.of());
        EntityType type2 = new EntityType("Building", "Другое название", Map.of("key", "value"));

        assertEquals(type1, type2);
        assertEquals(type1.hashCode(), type2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        EntityType type1 = new EntityType("Building", "Здание", Map.of());
        EntityType type2 = new EntityType("Contract", "Договор", Map.of());

        assertNotEquals(type1, type2);
    }

    @Test
    void shouldNotBeEqualWithNull() {
        EntityType type = new EntityType("Building", "Здание", Map.of());
        assertNotEquals(type, null);
    }

    @Test
    void shouldNotBeEqualWithDifferentClass() {
        EntityType type = new EntityType("Building", "Здание", Map.of());
        assertNotEquals(type, "Building");
    }

    @Test
    void shouldReturnCorrectToString() {
        EntityType type = new EntityType("Building", "Здание", Map.of());
        String toString = type.toString();

        assertTrue(toString.contains("Building"));
        assertTrue(toString.contains("Здание"));
        assertTrue(toString.contains("EntityType"));
    }
}

