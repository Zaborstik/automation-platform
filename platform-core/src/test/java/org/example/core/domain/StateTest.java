package org.example.core.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {

    @Test
    void shouldCreateStateWithAllFields() {
        Map<String, Object> properties = Map.of("status", "active", "version", 1);
        State state = new State("active", "Активное состояние", properties);

        assertEquals("active", state.getId());
        assertEquals("Активное состояние", state.getName());
        assertEquals(properties, state.getProperties());
    }

    @Test
    void shouldCreateStateWithNullProperties() {
        State state = new State("active", "Активное", null);

        assertEquals("active", state.getId());
        assertTrue(state.getProperties().isEmpty());
    }

    @Test
    void shouldCreateStateWithEmptyProperties() {
        State state = new State("active", "Активное", Map.of());

        assertEquals("active", state.getId());
        assertTrue(state.getProperties().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new State(null, "Name", Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new State("id", null, Map.of());
        });
    }

    @Test
    void shouldReturnImmutableProperties() {
        Map<String, Object> originalProperties = Map.of("key", "value");
        State state = new State("id", "name", originalProperties);

        Map<String, Object> returnedProperties = state.getProperties();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedProperties.put("newKey", "newValue");
        });
    }

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
        State state1 = new State("active", "Название1", Map.of("key1", "value1"));
        State state2 = new State("active", "Название2", Map.of("key2", "value2"));

        assertEquals(state1, state2);
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenIdsAreDifferent() {
        State state1 = new State("active", "Активное", Map.of());
        State state2 = new State("inactive", "Неактивное", Map.of());

        assertNotEquals(state1, state2);
    }

    @Test
    void shouldNotBeEqualWithNull() {
        State state = new State("active", "Активное", Map.of());
        assertNotEquals(state, null);
    }

    @Test
    void shouldNotBeEqualWithDifferentClass() {
        State state = new State("active", "Активное", Map.of());
        assertNotEquals(state, "active");
    }

    @Test
    void shouldReturnCorrectToString() {
        State state = new State("active", "Активное состояние", Map.of());
        String toString = state.toString();

        assertTrue(toString.contains("active"));
        assertTrue(toString.contains("Активное состояние"));
        assertTrue(toString.contains("State"));
    }
}

