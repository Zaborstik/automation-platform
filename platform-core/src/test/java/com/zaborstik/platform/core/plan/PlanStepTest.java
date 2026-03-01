package com.zaborstik.platform.core.plan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanStepTest {

    @Test
    void shouldCreatePlanStepWithAllFields() {
        List<PlanStepAction> actions = List.of(
            new PlanStepAction("act-click", null),
            new PlanStepAction("act-input-text", "текст")
        );
        PlanStep step = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-button",
            "btn-1",
            1,
            "Кнопка #btn-1: Клик",
            actions
        );
        assertEquals("step-1", step.id());
        assertEquals("plan-1", step.planId());
        assertEquals("wf-plan-step", step.workflowId());
        assertEquals("new", step.workflowStepInternalName());
        assertEquals("ent-button", step.entityTypeId());
        assertEquals("btn-1", step.entityId());
        assertEquals(1, step.sortOrder());
        assertEquals("Кнопка #btn-1: Клик", step.displayName());
        assertEquals(2, step.actions().size());
        assertEquals("act-click", step.actions().get(0).actionId());
        assertEquals("текст", step.actions().get(1).metaValue());
    }

    @Test
    void shouldCreatePlanStepWithNullEntityId() {
        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "new", "ent-page", null, 1, "Шаг", List.of()
        );
        assertNull(step.entityId());
    }

    @Test
    void shouldReturnImmutableActions() {
        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "new", "ent", "e1", 1, "Display",
            List.of(new PlanStepAction("act-1", null))
        );
        assertThrows(UnsupportedOperationException.class, () ->
            step.actions().add(new PlanStepAction("act-2", null))
        );
    }

    @Test
    void shouldThrowWhenIdNull() {
        assertThrows(NullPointerException.class, () ->
            new PlanStep(null, "p1", "wf", "new", "ent", "e1", 1, "D", List.of())
        );
    }
}
