package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    private InMemoryResolver resolver;
    private Planner planner;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        planner = new Planner(resolver);
    }

    @Test
    void shouldCreatePlanWithOneStepAndOneAction() {
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "Нажатие.", "act-type-1"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");

        ExecutionRequest request = new ExecutionRequest("ent-button", "btn-1", "act-click", Map.of());
        Plan plan = planner.createPlan(request);

        assertNotNull(plan);
        assertEquals(Planner.WORKFLOW_PLAN_ID, plan.workflowId());
        assertEquals(Planner.WORKFLOW_STEP_NEW, plan.workflowStepInternalName());
        assertNotNull(plan.stoppedAtPlanStepId());

        var steps = plan.steps();
        assertEquals(1, steps.size());
        PlanStep step = steps.get(0);
        assertEquals(plan.stoppedAtPlanStepId(), step.id());
        assertEquals("ent-button", step.entityTypeId());
        assertEquals("btn-1", step.entityId());
        assertEquals(1, step.sortOrder());
        assertEquals(Planner.WORKFLOW_PLAN_STEP_ID, step.workflowId());

        var actions = step.actions();
        assertEquals(1, actions.size());
        PlanStepAction psa = actions.get(0);
        assertEquals("act-click", psa.actionId());
    }

    @Test
    void shouldPassMetaValueFromRequest() {
        resolver.registerEntityType(EntityType.of("ent-input", "Поле ввода"));
        resolver.registerAction(Action.of("act-input-text", "Ввести текст", "input_text", "Ввод.", "act-type-1"));
        resolver.registerActionApplicableToEntityType("act-input-text", "ent-input");

        ExecutionRequest request = new ExecutionRequest(
            "ent-input", "input-1", "act-input-text",
            Map.of("meta_value", "поисковый запрос")
        );
        Plan plan = planner.createPlan(request);
        assertEquals("поисковый запрос", plan.steps().get(0).actions().get(0).metaValue());
    }

    @Test
    void shouldThrowWhenEntityTypeNotFound() {
        resolver.registerAction(Action.of("act-1", "A", "a", "D", "t1"));
        resolver.registerActionApplicableToEntityType("act-1", "ent-x");
        assertThrows(IllegalArgumentException.class, () ->
            planner.createPlan(new ExecutionRequest("NonExistent", "1", "act-1", Map.of()))
        );
    }

    @Test
    void shouldThrowWhenActionNotFound() {
        resolver.registerEntityType(EntityType.of("ent-1", "E1"));
        assertThrows(IllegalArgumentException.class, () ->
            planner.createPlan(new ExecutionRequest("ent-1", "1", "non_existent", Map.of()))
        );
    }

    @Test
    void shouldThrowWhenActionNotApplicableToEntityType() {
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "D", "t1"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-link"); // только для ent-link

        ExecutionRequest request = new ExecutionRequest("ent-button", "btn-1", "act-click", Map.of());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            planner.createPlan(request)
        );
        assertTrue(ex.getMessage().contains("not applicable"));
    }
}
