package com.zaborstik.platform.core.plan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanTest {

    @Test
    void shouldCreatePlanWithAllFields() {
        PlanStep step = new PlanStep(
            "step-1", "plan-1", "wf-plan-step", "new",
            "ent-button", "btn-1", 1, "Шаг 1",
            List.of(new PlanStepAction("act-click", null))
        );
        Plan plan = new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "Цель пользователя",
            "Пояснение",
            List.of(step)
        );
        assertEquals("plan-1", plan.id());
        assertEquals("wf-plan", plan.workflowId());
        assertEquals("new", plan.workflowStepInternalName());
        assertEquals("step-1", plan.stoppedAtPlanStepId());
        assertEquals("Цель пользователя", plan.target());
        assertEquals("Пояснение", plan.explanation());
        assertEquals(1, plan.steps().size());
        assertEquals("step-1", plan.steps().get(0).id());
    }

    @Test
    void shouldCreatePlanWithNullTargetAndExplanation() {
        Plan plan = new Plan("p1", "wf", "new", "s1", null, null, List.of());
        assertNull(plan.target());
        assertNull(plan.explanation());
    }

    @Test
    void shouldReturnImmutableSteps() {
        PlanStep step = new PlanStep("s1", "p1", "wf", "new", "ent", "e1", 1, "D", List.of());
        Plan plan = new Plan("p1", "wf", "new", "s1", null, null, List.of(step));
        assertThrows(UnsupportedOperationException.class, () ->
            plan.steps().add(step)
        );
    }

    @Test
    void shouldThrowWhenStoppedAtPlanStepIdNull() {
        assertThrows(NullPointerException.class, () ->
            new Plan("p1", "wf", "new", null, null, null, List.of())
        );
    }
}
