package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.knowledge.model.AppKnowledge;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.UIElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeElementResolverTest {

    private InMemoryKnowledgeRepository repository;
    private KnowledgeElementResolver resolver;

    @BeforeEach
    void setUp() {
        repository = new InMemoryKnowledgeRepository();
        resolver = new KnowledgeElementResolver(repository);
    }

    @Test
    void shouldFindSelectorByElementName() {
        PageKnowledge page = new PageKnowledge(
            "https://example.com/search",
            "Search",
            List.of(
                new UIElement("search_input", "input#search", "CSS", "input", "Search", java.util.Map.of()),
                new UIElement("submit_btn", "button[type=submit]", "CSS", "button", "Submit", java.util.Map.of())
            ),
            Instant.now()
        );
        AppKnowledge app = new AppKnowledge(
            "app-1",
            "TestApp",
            "https://example.com",
            List.of(page),
            Instant.now()
        );
        repository.save(app);

        assertEquals(Optional.of("input#search"), resolver.findSelectorByElementName("search_input", null));
        assertEquals(Optional.of("button[type=submit]"), resolver.findSelectorByElementName("submit_btn", null));
    }

    @Test
    void shouldReturnEmptyWhenElementNotFound() {
        assertTrue(resolver.findSelectorByElementName("nonexistent", null).isEmpty());
    }
}
