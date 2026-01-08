package com.zaborstik.platform.core.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ActionTest {

    @Test
    void shouldCreateActionWithAllFields() {
        Set<String> applicableTypes = Set.of("Building", "Contract");
        Map<String, Object> metadata = Map.of("category", "egrn");
        
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Описание действия",
            applicableTypes,
            metadata
        );

        assertEquals("order_egrn_extract", action.id());
        assertEquals("Заказать выписку из ЕГРН", action.name());
        assertEquals("Описание действия", action.description());
        assertEquals(applicableTypes, action.applicableEntityTypes());
        assertEquals(metadata, action.metadata());
    }

    @Test
    void shouldCreateActionWithNullDescription() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            null,
            Set.of("Building"),
            Map.of()
        );

        assertNull(action.description());
    }

    @Test
    void shouldCreateActionWithNullApplicableTypes() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            null,
            Map.of()
        );

        assertTrue(action.applicableEntityTypes().isEmpty());
    }

    @Test
    void shouldCreateActionWithNullMetadata() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            Set.of("Building"),
            null
        );

        assertTrue(action.metadata().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new Action(null, "Name", "Description", Set.of(), Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new Action("id", null, "Description", Set.of(), Map.of());
        });
    }

    @Test
    void shouldReturnImmutableApplicableTypes() {
        Set<String> originalTypes = Set.of("Building");
        Action action = new Action("id", "name", "desc", originalTypes, Map.of());

        Set<String> returnedTypes = action.applicableEntityTypes();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedTypes.add("Contract");
        });
    }

    @Test
    void shouldReturnImmutableMetadata() {
        Map<String, Object> originalMetadata = Map.of("key", "value");
        Action action = new Action("id", "name", "desc", Set.of(), originalMetadata);

        Map<String, Object> returnedMetadata = action.metadata();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedMetadata.put("newKey", "newValue");
        });
    }

    @Test
    void shouldCheckApplicabilityCorrectly() {
        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку",
            "Описание",
            Set.of("Building", "Contract"),
            Map.of()
        );

        assertTrue(action.isApplicableTo("Building"));
        assertTrue(action.isApplicableTo("Contract"));
        assertFalse(action.isApplicableTo("Extract"));
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        Action action1 = new Action("id", "Name1", "Desc1", Set.of("A"), Map.of());
        Action action2 = new Action("id", "Name2", "Desc2", Set.of("B"), Map.of("key", "value"));

        assertEquals(action1, action2);
        assertEquals(action1.hashCode(), action2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        Action action1 = new Action("id1", "Name", "Desc", Set.of(), Map.of());
        Action action2 = new Action("id2", "Name", "Desc", Set.of(), Map.of());

        assertNotEquals(action1, action2);
    }

    @Test
    void shouldNotBeEqualWithNull() {
        Action action = new Action("id", "name", "desc", Set.of(), Map.of());
        assertNotEquals(action, null);
    }

    @Test
    void shouldNotBeEqualWithDifferentClass() {
        Action action = new Action("id", "name", "desc", Set.of(), Map.of());
        assertNotEquals(action, "id");
    }

    @Test
    void shouldReturnCorrectToString() {
        Action action = new Action("order_egrn_extract", "Заказать выписку", "Описание", Set.of(), Map.of());
        String toString = action.toString();

        assertTrue(toString.contains("order_egrn_extract"));
        assertTrue(toString.contains("Заказать выписку"));
        assertTrue(toString.contains("Action"));
    }
}

