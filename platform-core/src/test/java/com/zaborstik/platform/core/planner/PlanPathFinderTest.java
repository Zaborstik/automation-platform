package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanPathFinderTest {

    private PlanPathFinder pathFinder;

    @BeforeEach
    void setUp() {
        InMemoryResolver resolver = new InMemoryResolver();

        resolver.registerEntityType(EntityType.of("ent-page", "Page"));
        resolver.registerEntityType(EntityType.of("ent-button", "Button"));
        resolver.registerEntityType(EntityType.of("ent-form", "Form"));
        resolver.registerEntityType(EntityType.of("ent-table", "Table"));
        resolver.registerEntityType(EntityType.of("ent-link", "Link"));

        resolver.registerAction(Action.of("open_page", "Open page", "open_page", "Desc", "navigation"));
        resolver.registerAction(Action.of("wait_element", "Wait element", "wait_element", "Desc", "validation"));
        resolver.registerAction(Action.of("read_text", "Read text", "read_text", "Desc", "validation"));
        resolver.registerAction(Action.of("take_screenshot", "Take screenshot", "take_screenshot", "Desc", "artifact"));
        resolver.registerAction(Action.of("click", "Click", "click", "Desc", "interaction"));

        resolver.registerActionApplicableToEntityType("open_page", "ent-page");
        resolver.registerActionApplicableToEntityType("wait_element", "ent-page");
        resolver.registerActionApplicableToEntityType("wait_element", "ent-form");
        resolver.registerActionApplicableToEntityType("read_text", "ent-page");
        resolver.registerActionApplicableToEntityType("read_text", "ent-table");
        resolver.registerActionApplicableToEntityType("take_screenshot", "ent-page");
        resolver.registerActionApplicableToEntityType("click", "ent-button");
        resolver.registerActionApplicableToEntityType("click", "ent-link");

        pathFinder = new PlanPathFinder(resolver);
    }

    @Test
    void shouldFindApplicableActionsForPage() {
        Set<String> actionIds = pathFinder.findApplicableActions("ent-page")
            .stream()
            .map(Action::id)
            .collect(Collectors.toSet());

        assertEquals(Set.of("open_page", "wait_element", "read_text", "take_screenshot"), actionIds);
    }

    @Test
    void shouldFindApplicableActionsForButton() {
        Set<String> actionIds = pathFinder.findApplicableActions("ent-button")
            .stream()
            .map(Action::id)
            .collect(Collectors.toSet());

        assertEquals(Set.of("click"), actionIds);
    }

    @Test
    void shouldBuildActionSequenceForValidPairs() {
        List<PlanStepAction> sequence = pathFinder.buildActionSequence(
            List.of("ent-page", "ent-button"),
            List.of("open_page", "click")
        );

        assertEquals(2, sequence.size());
        assertEquals("open_page", sequence.get(0).actionId());
        assertEquals("click", sequence.get(1).actionId());
        assertNull(sequence.get(0).metaValue());
    }

    @Test
    void shouldThrowWhenActionIsNotApplicable() {
        assertThrows(IllegalArgumentException.class, () ->
            pathFinder.buildActionSequence(List.of("ent-button"), List.of("open_page"))
        );
    }

    @Test
    void canExecuteShouldReturnTrueOrFalse() {
        assertTrue(pathFinder.canExecute("click", "ent-button"));
        assertFalse(pathFinder.canExecute("open_page", "ent-button"));
    }
}
