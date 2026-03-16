package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Проверяет целостность и корректность плана перед исполнением.
 */
public class PlanValidator {

    private final Resolver resolver;

    public PlanValidator(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public List<String> validate(Plan plan) {
        List<String> errors = new ArrayList<>();
        if (plan == null) {
            errors.add("Plan cannot be null");
            return errors;
        }

        if (isBlank(plan.id())) {
            errors.add("Plan id cannot be empty");
        }

        if (isBlank(plan.workflowId())) {
            errors.add("Plan workflowId cannot be empty");
        } else if (resolver.findWorkflow(plan.workflowId()).isEmpty()) {
            errors.add("Workflow not found: " + plan.workflowId());
        }

        List<PlanStep> steps = plan.steps();
        if (steps == null || steps.isEmpty()) {
            errors.add("Plan must contain at least one step");
            return errors;
        }

        validateSortOrder(steps, errors);
        validateSteps(steps, errors);

        return errors;
    }

    public void validateOrThrow(Plan plan) {
        List<String> errors = validate(plan);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private void validateSteps(List<PlanStep> steps, List<String> errors) {
        for (PlanStep step : steps) {
            if (step.actions() == null || step.actions().isEmpty()) {
                errors.add("Step '" + step.id() + "' must contain at least one action");
                continue;
            }
            for (PlanStepAction action : step.actions()) {
                if (!resolver.isActionApplicable(action.actionId(), step.entityTypeId())) {
                    errors.add("Action '" + action.actionId()
                        + "' is not applicable to entity type '" + step.entityTypeId()
                        + "' in step '" + step.id() + "'");
                }
            }
        }
    }

    private static void validateSortOrder(List<PlanStep> steps, List<String> errors) {
        Set<Integer> seen = new HashSet<>();
        for (PlanStep step : steps) {
            if (!seen.add(step.sortOrder())) {
                errors.add("Plan step sortOrder must be unique");
                break;
            }
        }

        List<Integer> sorted = steps.stream().map(PlanStep::sortOrder).sorted().toList();
        for (int i = 0; i < sorted.size(); i++) {
            int expected = i + 1;
            if (sorted.get(i) != expected) {
                errors.add("Plan step sortOrder must be sequential starting from 1");
                break;
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
