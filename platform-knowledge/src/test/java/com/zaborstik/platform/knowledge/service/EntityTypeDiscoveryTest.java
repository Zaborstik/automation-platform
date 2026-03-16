package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.UIElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTypeDiscoveryTest {

    private EntityTypeDiscovery discovery;

    @BeforeEach
    void setUp() {
        InMemoryResolver resolver = new InMemoryResolver();
        resolver.registerEntityType(EntityType.of("ent-input", "Input"));
        resolver.registerEntityType(EntityType.of("ent-button", "Button"));
        resolver.registerAction(Action.of("act-input-text", "Input text", "input_text", "Desc", "act-type-data-input"));
        resolver.registerAction(Action.of("act-click", "Click", "click", "Desc", "act-type-interaction"));
        resolver.registerActionApplicableToEntityType("act-input-text", "ent-input");
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");
        discovery = new EntityTypeDiscovery(resolver);
    }

    @Test
    void shouldReturnActionsForInputAndButtonEntityTypes() {
        PageKnowledge page = new PageKnowledge(
            "/p",
            "title",
            List.of(
                new UIElement("input#q", "CSS", "input", "q", Map.of()),
                new UIElement("button#b", "CSS", "button", "b", Map.of())
            ),
            Instant.now()
        );

        Map<String, List<String>> result = discovery.discoverApplicableActions(page);

        assertTrue(result.containsKey("ent-input"));
        assertTrue(result.containsKey("ent-button"));
        assertTrue(result.get("ent-input").contains("act-input-text"));
        assertTrue(result.get("ent-button").contains("act-click"));
    }

    @Test
    void shouldReturnEmptyMapForEmptyPage() {
        PageKnowledge page = new PageKnowledge("/p", "title", List.of(), Instant.now());
        Map<String, List<String>> result = discovery.discoverApplicableActions(page);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSkipUnknownElementType() {
        PageKnowledge page = new PageKnowledge(
            "/p",
            "title",
            List.of(new UIElement("x", "CSS", "unknown", null, Map.of())),
            Instant.now()
        );

        Map<String, List<String>> result = discovery.discoverApplicableActions(page);
        assertEquals(0, result.size());
    }
}
