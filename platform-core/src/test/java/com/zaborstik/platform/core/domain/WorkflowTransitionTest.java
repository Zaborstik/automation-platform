package com.zaborstik.platform.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowTransitionTest {

    @Test
    void shouldCreateWorkflowTransition() {
        WorkflowTransition transition = new WorkflowTransition("wf-plan", "new", "in_progress");

        assertEquals("wf-plan", transition.workflowId());
        assertEquals("new", transition.fromStepInternalName());
        assertEquals("in_progress", transition.toStepInternalName());
    }

    @Test
    void shouldThrowWhenWorkflowIdIsNull() {
        assertThrows(NullPointerException.class, () ->
            new WorkflowTransition(null, "new", "in_progress")
        );
    }

    @Test
    void shouldThrowWhenFromStepInternalNameIsNull() {
        assertThrows(NullPointerException.class, () ->
            new WorkflowTransition("wf-plan", null, "in_progress")
        );
    }

    @Test
    void shouldThrowWhenToStepInternalNameIsNull() {
        assertThrows(NullPointerException.class, () ->
            new WorkflowTransition("wf-plan", "new", null)
        );
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        WorkflowTransition left = new WorkflowTransition("wf-plan", "new", "in_progress");
        WorkflowTransition same = new WorkflowTransition("wf-plan", "new", "in_progress");
        WorkflowTransition different = new WorkflowTransition("wf-plan", "in_progress", "completed");

        assertEquals(left, same);
        assertEquals(left.hashCode(), same.hashCode());
        assertNotEquals(left, different);
    }

    @Test
    void shouldIncludeAllFieldsInToString() {
        WorkflowTransition transition = new WorkflowTransition("wf-plan", "new", "in_progress");
        String text = transition.toString();

        assertTrue(text.contains("wf-plan"));
        assertTrue(text.contains("new"));
        assertTrue(text.contains("in_progress"));
    }
}
