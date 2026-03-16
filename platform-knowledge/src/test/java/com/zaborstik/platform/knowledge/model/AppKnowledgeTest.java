package com.zaborstik.platform.knowledge.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppKnowledgeTest {

    @Test
    void shouldCreateWithValidData() {
        PageKnowledge page = new PageKnowledge("/search", "Search", List.of(), Instant.now());
        AppKnowledge knowledge = new AppKnowledge("app-1", "DuckDuckGo", "https://duckduckgo.com", List.of(page), Instant.now());

        assertEquals("app-1", knowledge.appId());
        assertEquals("DuckDuckGo", knowledge.appName());
        assertEquals(1, knowledge.pages().size());
    }

    @Test
    void shouldThrowWhenRequiredFieldIsNull() {
        assertThrows(NullPointerException.class, () -> new AppKnowledge(null, "app", "url", List.of(), Instant.now()));
        assertThrows(NullPointerException.class, () -> new AppKnowledge("id", null, "url", List.of(), Instant.now()));
        assertThrows(NullPointerException.class, () -> new AppKnowledge("id", "app", null, List.of(), Instant.now()));
    }

    @Test
    void shouldExposeImmutablePages() {
        AppKnowledge knowledge = new AppKnowledge("id", "app", "url", new ArrayList<>(), Instant.now());
        assertThrows(UnsupportedOperationException.class, () -> knowledge.pages().add(new PageKnowledge("/", null, List.of(), Instant.now())));
    }

    @Test
    void toStringShouldContainKeyFields() {
        AppKnowledge knowledge = new AppKnowledge("id-1", "TestApp", "https://example.com", List.of(), Instant.now());
        String value = knowledge.toString();
        assertTrue(value.contains("id-1"));
        assertTrue(value.contains("TestApp"));
        assertTrue(value.contains("https://example.com"));
    }
}
