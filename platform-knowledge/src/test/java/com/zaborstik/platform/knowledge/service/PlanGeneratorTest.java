package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import com.zaborstik.platform.knowledge.model.ParsedUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanGeneratorTest {

    private InMemoryResolver resolver;
    private PlanGenerator generator;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        resolver.registerEntityType(EntityType.of("ent-input", "Input"));
        resolver.registerAction(Action.of("act-input-text", "Input", "input_text", "Desc", "act-type-data-input"));
        resolver.registerAction(Action.of("act-click", "Click", "click", "Desc", "act-type-interaction"));
        resolver.registerActionApplicableToEntityType("act-input-text", "ent-input");
        resolver.registerActionApplicableToEntityType("act-click", "ent-input");
        generator = new PlanGenerator(resolver);
    }

    @Test
    void shouldGeneratePlanWithApplicableActions() {
        ParsedUserRequest request = new ParsedUserRequest(
            "введи java и нажми",
            "ent-input",
            List.of("act-input-text", "act-click"),
            Map.of("meta_value", "java"),
            false,
            null
        );

        Plan plan = generator.generate(request);

        assertEquals("wf-plan", plan.workflowId());
        assertEquals("new", plan.workflowStepInternalName());
        assertEquals(2, plan.steps().size());
        assertEquals(1, plan.steps().get(0).sortOrder());
        assertEquals(2, plan.steps().get(1).sortOrder());
    }

    @Test
    void shouldThrowForNonApplicableAction() {
        ParsedUserRequest request = new ParsedUserRequest(
            "do action",
            "ent-input",
            List.of("act-unknown"),
            Map.of(),
            false,
            null
        );

        assertThrows(IllegalArgumentException.class, () -> generator.generate(request));
    }

    @Test
    void shouldThrowForEmptyActions() {
        ParsedUserRequest request = new ParsedUserRequest("do", "ent-input", List.of(), Map.of(), false, null);
        assertThrows(IllegalArgumentException.class, () -> generator.generate(request));
    }

    @Test
    void shouldCreateMultipleStepsWithCorrectOrder() {
        ParsedUserRequest request = new ParsedUserRequest(
            "run sequence",
            "ent-input",
            List.of("act-click", "act-input-text"),
            Map.of("meta_value", "x"),
            false,
            null
        );
        Plan plan = generator.generate(request);
        assertEquals(2, plan.steps().size());
        assertEquals(1, plan.steps().get(0).sortOrder());
        assertEquals(2, plan.steps().get(1).sortOrder());
    }
}
