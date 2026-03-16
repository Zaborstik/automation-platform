package com.zaborstik.platform.knowledge.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedUserRequestTest {

    @Test
    void shouldCreateWithValidData() {
        ParsedUserRequest request = new ParsedUserRequest(
            "find me results",
            "ent-input",
            List.of("act-input-text", "act-click"),
            Map.of("meta_value", "java"),
            false,
            null
        );

        assertEquals("find me results", request.rawInput());
        assertEquals("ent-input", request.entityTypeId());
        assertEquals(2, request.actionIds().size());
        assertFalse(request.clarificationNeeded());
    }

    @Test
    void shouldThrowWhenRawInputIsNull() {
        assertThrows(NullPointerException.class, () ->
            new ParsedUserRequest(null, "ent-input", List.of(), Map.of(), false, null)
        );
    }

    @Test
    void shouldExposeImmutableCollections() {
        ParsedUserRequest request = new ParsedUserRequest("x", "ent-input", new ArrayList<>(), new HashMap<>(), false, null);
        assertThrows(UnsupportedOperationException.class, () -> request.actionIds().add("act-click"));
        assertThrows(UnsupportedOperationException.class, () -> request.parameters().put("k", "v"));
    }

    @Test
    void toStringShouldContainKeyFields() {
        ParsedUserRequest request = new ParsedUserRequest("do search", "ent-input", List.of("act-click"), Map.of(), true, "What to search?");
        String value = request.toString();
        assertTrue(value.contains("do search"));
        assertTrue(value.contains("ent-input"));
        assertTrue(value.contains("act-click"));
    }
}
