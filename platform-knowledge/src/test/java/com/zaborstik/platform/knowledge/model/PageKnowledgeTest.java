package com.zaborstik.platform.knowledge.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageKnowledgeTest {

    @Test
    void shouldCreateWithValidData() {
        UIElement element = new UIElement("q", "#q", "CSS", "input", "Search", java.util.Map.of());
        PageKnowledge page = new PageKnowledge("/search", "Search", List.of(element), Instant.now());

        assertEquals("/search", page.pageUrl());
        assertEquals("Search", page.pageTitle());
        assertEquals(1, page.elements().size());
    }

    @Test
    void shouldThrowWhenPageUrlIsNull() {
        assertThrows(NullPointerException.class, () -> new PageKnowledge(null, "Search", List.of(), Instant.now()));
    }

    @Test
    void shouldExposeImmutableElements() {
        PageKnowledge page = new PageKnowledge("/x", "t", new ArrayList<>(), Instant.now());
        assertThrows(UnsupportedOperationException.class,
            () -> page.elements().add(new UIElement("id", "#id", "CSS", "input", null, java.util.Map.of())));
    }

    @Test
    void toStringShouldContainKeyFields() {
        PageKnowledge page = new PageKnowledge("/search", "Search", List.of(), Instant.now());
        String value = page.toString();
        assertTrue(value.contains("/search"));
        assertTrue(value.contains("Search"));
    }
}
