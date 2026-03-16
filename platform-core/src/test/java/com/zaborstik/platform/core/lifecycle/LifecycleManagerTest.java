package com.zaborstik.platform.core.lifecycle;

import com.zaborstik.platform.core.domain.WorkflowTransition;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleManagerTest {

    private LifecycleManager lifecycleManager;

    @BeforeEach
    void setUp() {
        InMemoryResolver resolver = new InMemoryResolver();
        resolver.registerTransition(new WorkflowTransition("wf-plan", "new", "in_progress"));
        resolver.registerTransition(new WorkflowTransition("wf-plan", "in_progress", "completed"));
        resolver.registerTransition(new WorkflowTransition("wf-plan", "in_progress", "failed"));
        lifecycleManager = new LifecycleManager(resolver);
    }

    @Test
    void canTransitionShouldReturnTrueForAllowedTransition() {
        assertTrue(lifecycleManager.canTransition("wf-plan", "new", "in_progress"));
    }

    @Test
    void canTransitionShouldReturnFalseForForbiddenTransition() {
        assertFalse(lifecycleManager.canTransition("wf-plan", "completed", "new"));
    }

    @Test
    void validateTransitionShouldNotThrowForAllowedTransition() {
        assertDoesNotThrow(() -> lifecycleManager.validateTransition("wf-plan", "new", "in_progress"));
    }

    @Test
    void validateTransitionShouldThrowForForbiddenTransition() {
        assertThrows(IllegalStateException.class, () ->
            lifecycleManager.validateTransition("wf-plan", "completed", "new")
        );
    }

    @Test
    void getNextStepShouldReturnFirstAllowedTargetStep() {
        assertEquals("in_progress", lifecycleManager.getNextStep("wf-plan", "new"));
    }

    @Test
    void getNextStepShouldThrowWhenNoTransitionExists() {
        assertThrows(IllegalStateException.class, () ->
            lifecycleManager.getNextStep("wf-plan", "cancelled")
        );
    }

    @Test
    void shouldThrowNullPointerExceptionWhenArgumentsAreNull() {
        assertThrows(NullPointerException.class, () -> new LifecycleManager(null));
        assertThrows(NullPointerException.class, () -> lifecycleManager.canTransition(null, "new", "in_progress"));
        assertThrows(NullPointerException.class, () -> lifecycleManager.canTransition("wf-plan", null, "in_progress"));
        assertThrows(NullPointerException.class, () -> lifecycleManager.canTransition("wf-plan", "new", null));
        assertThrows(NullPointerException.class, () -> lifecycleManager.validateTransition(null, "new", "in_progress"));
        assertThrows(NullPointerException.class, () -> lifecycleManager.getNextStep(null, "new"));
        assertThrows(NullPointerException.class, () -> lifecycleManager.getNextStep("wf-plan", null));
    }
}
