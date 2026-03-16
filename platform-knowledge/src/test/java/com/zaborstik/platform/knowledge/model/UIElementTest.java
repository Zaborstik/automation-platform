package com.zaborstik.platform.knowledge.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UIElementTest {

    @Test
    void shouldCreateWithValidData() {
        UIElement element = new UIElement("#search", "CSS", "input", "Search", Map.of("name", "q"));
        assertEquals("#search", element.selector());
        assertEquals("input", element.elementType());
        assertEquals("q", element.attributes().get("name"));
    }

    @Test
    void shouldThrowWhenRequiredFieldIsNull() {
        assertThrows(NullPointerException.class, () -> new UIElement(null, "CSS", "input", null, Map.of()));
        assertThrows(NullPointerException.class, () -> new UIElement("#x", "CSS", null, null, Map.of()));
    }

    @Test
    void shouldExposeImmutableAttributes() {
        UIElement element = new UIElement("#x", "CSS", "button", "Run", new HashMap<>());
        assertThrows(UnsupportedOperationException.class, () -> element.attributes().put("type", "submit"));
    }

    @Test
    void toStringShouldContainKeyFields() {
        UIElement element = new UIElement("#search", "CSS", "input", "Search", Map.of());
        String value = element.toString();
        assertTrue(value.contains("#search"));
        assertTrue(value.contains("input"));
    }
}
