package com.zaborstik.platform.core;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionEngineTest {

    private InMemoryResolver resolver;
    private ExecutionEngine engine;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        engine = new ExecutionEngine(resolver);
    }

    @Test
    void shouldCreatePlanSuccessfully() {
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "Описание", "act-type-1"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");

        ExecutionRequest request = new ExecutionRequest("ent-button", "btn-1", "act-click", Map.of());
        Plan plan = engine.createPlan(request);

        assertNotNull(plan);
        assertNotNull(plan.id());
        assertEquals(1, plan.steps().size());
        assertEquals("ent-button", plan.steps().get(0).entityTypeId());
        assertEquals("btn-1", plan.steps().get(0).entityId());
        assertEquals("act-click", plan.steps().get(0).actions().get(0).actionId());
    }

    @Test
    void shouldThrowWhenEntityTypeNotFound() {
        resolver.registerAction(Action.of("act-1", "A", "a", "D", "t1"));
        resolver.registerActionApplicableToEntityType("act-1", "ent-x");
        assertThrows(IllegalArgumentException.class, () ->
            engine.createPlan(new ExecutionRequest("NonExistent", "1", "act-1", Map.of()))
        );
    }

    @Test
    void shouldThrowWhenActionNotApplicable() {
        resolver.registerEntityType(EntityType.of("ent-1", "E1"));
        resolver.registerAction(Action.of("act-1", "A", "a", "D", "t1"));
        resolver.registerActionApplicableToEntityType("act-1", "ent-2");
        assertThrows(IllegalArgumentException.class, () ->
            engine.createPlan(new ExecutionRequest("ent-1", "1", "act-1", Map.of()))
        );
    }

    @Test
    void shouldHandleMultipleRequests() {
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "D", "t1"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");

        Plan plan1 = engine.createPlan(new ExecutionRequest("ent-button", "123", "act-click", Map.of()));
        Plan plan2 = engine.createPlan(new ExecutionRequest("ent-button", "456", "act-click", Map.of()));
        assertNotEquals(plan1.id(), plan2.id());
        assertEquals("123", plan1.steps().get(0).entityId());
        assertEquals("456", plan2.steps().get(0).entityId());
    }
}
