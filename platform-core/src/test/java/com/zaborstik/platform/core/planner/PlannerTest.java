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

import java.util.List;
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

    @Test
    void shouldCreateMultiStepPlanWithOrderedSteps() {
        resolver.registerEntityType(EntityType.of("ent-page", "Страница"));
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerEntityType(EntityType.of("ent-input", "Поле ввода"));
        resolver.registerAction(Action.of("open_page", "Открыть страницу", "open_page", "D", "t-nav"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "D", "t-int"));
        resolver.registerAction(Action.of("input_text", "Ввести текст", "input_text", "D", "t-input"));
        resolver.registerActionApplicableToEntityType("open_page", "ent-page");
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");
        resolver.registerActionApplicableToEntityType("input_text", "ent-input");

        List<ExecutionRequest> requests = List.of(
            new ExecutionRequest("ent-page", "page-1", "open_page", Map.of()),
            new ExecutionRequest("ent-button", "btn-1", "act-click", Map.of()),
            new ExecutionRequest("ent-input", "input-1", "input_text", Map.of("meta_value", "hello"))
        );

        Plan plan = planner.createMultiStepPlan("Target", "Explanation", requests);

        assertEquals(3, plan.steps().size());
        assertEquals(1, plan.steps().get(0).sortOrder());
        assertEquals(2, plan.steps().get(1).sortOrder());
        assertEquals(3, plan.steps().get(2).sortOrder());

        assertEquals("ent-page", plan.steps().get(0).entityTypeId());
        assertEquals("open_page", plan.steps().get(0).actions().get(0).actionId());
        assertEquals("ent-button", plan.steps().get(1).entityTypeId());
        assertEquals("act-click", plan.steps().get(1).actions().get(0).actionId());
        assertEquals("ent-input", plan.steps().get(2).entityTypeId());
        assertEquals("input_text", plan.steps().get(2).actions().get(0).actionId());

        assertEquals(plan.steps().get(0).id(), plan.stoppedAtPlanStepId());
    }

    @Test
    void shouldThrowWhenCreateMultiStepPlanWithEmptyRequests() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            planner.createMultiStepPlan("Target", "Explanation", List.of())
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void shouldThrowWhenOneOfMultiStepRequestsHasNonApplicableAction() {
        resolver.registerEntityType(EntityType.of("ent-page", "Страница"));
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("open_page", "Открыть страницу", "open_page", "D", "t-nav"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "D", "t-int"));
        resolver.registerActionApplicableToEntityType("open_page", "ent-page");
        resolver.registerActionApplicableToEntityType("act-click", "ent-link");

        List<ExecutionRequest> requests = List.of(
            new ExecutionRequest("ent-page", "page-1", "open_page", Map.of()),
            new ExecutionRequest("ent-button", "btn-1", "act-click", Map.of())
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            planner.createMultiStepPlan("Target", "Explanation", requests)
        );
        assertTrue(ex.getMessage().contains("not applicable"));
    }
}
