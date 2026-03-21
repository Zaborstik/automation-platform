package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.WorkflowTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryResolverTest {

    private InMemoryResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
    }

    @Test
    void shouldRegisterAndFindEntityType() {
        EntityType entityType = EntityType.of("ent-button", "Кнопка");
        resolver.registerEntityType(entityType);
        Optional<EntityType> found = resolver.findEntityType("ent-button");
        assertTrue(found.isPresent());
        assertEquals(entityType, found.get());
    }

    @Test
    void shouldRegisterAndFindAction() {
        Action action = Action.of("act-click", "Клик", "click", "Описание", "act-type-interaction");
        resolver.registerAction(action);
        Optional<Action> found = resolver.findAction("act-click");
        assertTrue(found.isPresent());
        assertEquals(action, found.get());
    }

    @Test
    void shouldRegisterAndFindActionApplicableToEntityType() {
        resolver.registerEntityType(EntityType.of("ent-button", "Кнопка"));
        resolver.registerAction(Action.of("act-click", "Клик", "click", "Desc", "act-type-1"));
        resolver.registerActionApplicableToEntityType("act-click", "ent-button");

        assertTrue(resolver.isActionApplicable("act-click", "ent-button"));
        assertFalse(resolver.isActionApplicable("act-click", "ent-link"));

        List<Action> applicable = resolver.findActionsApplicableToEntityType("ent-button");
        assertEquals(1, applicable.size());
        assertEquals("act-click", applicable.get(0).id());
    }

    @Test
    void shouldReturnEmptyWhenEntityTypeNotFound() {
        assertFalse(resolver.findEntityType("non_existent").isPresent());
    }

    @Test
    void shouldReturnEmptyWhenActionNotFound() {
        assertFalse(resolver.findAction("non_existent").isPresent());
    }

    @Test
    void shouldRegisterAndFindUIBinding() {
        UIBinding uiBinding = new UIBinding("act-click", "[data-action='submit']", UIBinding.SelectorType.CSS, java.util.Map.of());
        resolver.registerUIBinding(uiBinding);
        Optional<UIBinding> found = resolver.findUIBinding("act-click");
        assertTrue(found.isPresent());
    }

    @Test
    void shouldFindWorkflowAndWorkflowStep() {
        resolver.registerWorkflowStep(new com.zaborstik.platform.core.domain.WorkflowStep("wfs-new", "new", "Новая", 10));
        resolver.registerWorkflow(new com.zaborstik.platform.core.domain.Workflow("wf-plan", "ЖЦ плана", "wfs-new"));

        assertTrue(resolver.findWorkflowStep("wfs-new").isPresent());
        assertTrue(resolver.findWorkflow("wf-plan").isPresent());
        assertTrue(resolver.findWorkflowStepByInternalName("new").isPresent());
    }

    @Test
    void isWorkflowStepInternalNameShouldUseRegisteredSteps() {
        assertFalse(resolver.isWorkflowStepInternalName("new"));
        resolver.registerWorkflowStep(new com.zaborstik.platform.core.domain.WorkflowStep("wfs-new", "new", "Новая", 10));
        assertTrue(resolver.isWorkflowStepInternalName("new"));
        assertFalse(resolver.isWorkflowStepInternalName("unknown"));
    }

    @Test
    void shouldRegisterAndFindTransitions() {
        resolver.registerTransition(new WorkflowTransition("wf-plan", "new", "in_progress"));
        resolver.registerTransition(new WorkflowTransition("wf-plan", "in_progress", "completed"));
        resolver.registerTransition(new WorkflowTransition("wf-plan", "in_progress", "failed"));

        List<WorkflowTransition> transitions = resolver.findTransitions("wf-plan");
        assertEquals(3, transitions.size());

        assertTrue(resolver.findTransition("wf-plan", "new", "in_progress").isPresent());
        assertTrue(resolver.findTransition("wf-plan", "in_progress", "completed").isPresent());
        assertFalse(resolver.findTransition("wf-plan", "completed", "new").isPresent());
    }

    @Test
    void shouldReturnEmptyTransitionsForUnknownWorkflow() {
        assertEquals(List.of(), resolver.findTransitions("nonexistent"));
    }
}
