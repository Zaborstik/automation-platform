package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanValidatorTest {

    private PlanValidator validator;

    @BeforeEach
    void setUp() {
        InMemoryResolver resolver = new InMemoryResolver();
        resolver.registerWorkflow(new Workflow("wf-plan", "Plan workflow", "wfs-new"));
        resolver.registerEntityType(EntityType.of("ent-page", "Page"));
        resolver.registerEntityType(EntityType.of("ent-button", "Button"));
        resolver.registerAction(Action.of("open_page", "Open page", "open_page", "Open page.", "navigation"));
        resolver.registerAction(Action.of("click", "Click", "click", "Click element.", "interaction"));
        resolver.registerActionApplicableToEntityType("open_page", "ent-page");
        resolver.registerActionApplicableToEntityType("click", "ent-button");
        validator = new PlanValidator(resolver);
    }

    @Test
    void shouldReturnNoErrorsForValidPlan() {
        Plan plan = validPlan();
        List<String> errors = validator.validate(plan);
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldReturnErrorWhenPlanHasNoSteps() {
        Plan plan = new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of()
        );
        List<String> errors = validator.validate(plan);
        assertTrue(errors.stream().anyMatch(message -> message.contains("at least one step")));
    }

    @Test
    void shouldReturnErrorWhenStepHasNoActions() {
        PlanStep stepWithoutActions = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-page",
            "page-1",
            1,
            "Step without actions",
            List.of()
        );
        Plan plan = new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(stepWithoutActions)
        );

        List<String> errors = validator.validate(plan);
        assertTrue(errors.stream().anyMatch(message -> message.contains("at least one action")));
    }

    @Test
    void shouldReturnErrorWhenActionIsNotApplicable() {
        PlanStep stepWithWrongAction = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-page",
            "page-1",
            1,
            "Wrong action",
            List.of(new PlanStepAction("click", null))
        );
        Plan plan = new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(stepWithWrongAction)
        );

        List<String> errors = validator.validate(plan);
        assertTrue(errors.stream().anyMatch(message -> message.contains("not applicable")));
    }

    @Test
    void shouldReturnMultipleErrors() {
        PlanStep firstStep = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-page",
            "page-1",
            1,
            "Step one",
            List.of(new PlanStepAction("click", null))
        );
        PlanStep secondStep = new PlanStep(
            "step-2",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-button",
            "button-1",
            1,
            "Step two",
            List.of()
        );
        Plan plan = new Plan(
            "plan-1",
            "wf-missing",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(firstStep, secondStep)
        );

        List<String> errors = validator.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(message -> message.contains("Workflow not found")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("sortOrder must be unique")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("sortOrder must be sequential")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("not applicable")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("at least one action")));
    }

    @Test
    void validateOrThrowShouldThrowWithErrorList() {
        Plan invalid = new Plan(
            "plan-1",
            "wf-missing",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            validator.validateOrThrow(invalid)
        );

        assertTrue(exception.getMessage().contains("Workflow not found"));
        assertTrue(exception.getMessage().contains("at least one step"));
    }

    @Test
    void shouldReturnErrorForNullPlan() {
        List<String> errors = validator.validate(null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("cannot be null"));
    }

    private static Plan validPlan() {
        PlanStep firstStep = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-page",
            "page-1",
            1,
            "Open page",
            List.of(new PlanStepAction("open_page", null))
        );
        PlanStep secondStep = new PlanStep(
            "step-2",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-button",
            "button-1",
            2,
            "Click button",
            List.of(new PlanStepAction("click", null))
        );
        return new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(firstStep, secondStep)
        );
    }
}
