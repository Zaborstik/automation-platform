package com.zaborstik.platform.core.plan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanUpdateRequestTest {

    @Test
    void shouldCreateWithAllFields() {
        PlanUpdateRequest request = new PlanUpdateRequest("plan-1", "in_progress", "step-2");

        assertEquals("plan-1", request.planId());
        assertEquals("in_progress", request.newWorkflowStepInternalName());
        assertEquals("step-2", request.stoppedAtPlanStepId());
    }

    @Test
    void shouldCreateWithNullStoppedAtPlanStepId() {
        PlanUpdateRequest request = new PlanUpdateRequest("plan-1", "in_progress", null);

        assertEquals("plan-1", request.planId());
        assertEquals("in_progress", request.newWorkflowStepInternalName());
        assertNull(request.stoppedAtPlanStepId());
    }

    @Test
    void shouldThrowWhenPlanIdIsNull() {
        assertThrows(NullPointerException.class, () ->
            new PlanUpdateRequest(null, "in_progress", "step-1")
        );
    }

    @Test
    void shouldThrowWhenNewWorkflowStepInternalNameIsNull() {
        assertThrows(NullPointerException.class, () ->
            new PlanUpdateRequest("plan-1", null, "step-1")
        );
    }
}
