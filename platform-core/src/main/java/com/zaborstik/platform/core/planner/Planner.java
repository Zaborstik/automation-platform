package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;

import java.util.List;
import java.util.UUID;

/**
 * Строит план выполнения по запросу пользователя.
 * План имеет ЖЦ (workflow + workflowStepInternalName), шаги (plan_step) с действиями (plan_step_action).
 */
public class Planner {

    /** Идентификатор ЖЦ плана (system.workflow). */
    public static final String WORKFLOW_PLAN_ID = "wf-plan";
    /** Идентификатор ЖЦ шага плана. */
    public static final String WORKFLOW_PLAN_STEP_ID = "wf-plan-step";
    /** Первый шаг ЖЦ: новая. */
    public static final String WORKFLOW_STEP_NEW = "new";

    private final Resolver resolver;

    public Planner(Resolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Создаёт план по запросу: один шаг плана (entity_type + entity_id) с одним действием (action).
     * Применимость действия к типу сущности проверяется по action_applicable_entity_type.
     */
    public Plan createPlan(ExecutionRequest request) {
        EntityType entityType = resolver.findEntityType(request.entityType())
            .orElseThrow(() -> new IllegalArgumentException("EntityType not found: " + request.entityType()));

        Action action = resolver.findAction(request.action())
            .orElseThrow(() -> new IllegalArgumentException("Action not found: " + request.action()));

        if (!resolver.isActionApplicable(action.id(), entityType.id())) {
            throw new IllegalArgumentException(
                "Action '" + action.id() + "' is not applicable to entity type '" + entityType.id() + "'");
        }

        String planId = UUID.randomUUID().toString();
        String stepId = UUID.randomUUID().toString();

        String metaValue = request.parameters().get("meta_value") != null
            ? String.valueOf(request.parameters().get("meta_value"))
            : null;

        PlanStep step = new PlanStep(
            stepId,
            planId,
            WORKFLOW_PLAN_STEP_ID,
            WORKFLOW_STEP_NEW,
            entityType.id(),
            request.entityId(),
            1,
            buildStepDisplayName(entityType, action, request),
            List.of(new PlanStepAction(action.id(), metaValue))
        );

        String target = request.parameters().get("target") != null
            ? String.valueOf(request.parameters().get("target"))
            : null;
        String explanation = action.description() != null ? action.description() : action.displayName();

        return new Plan(
            planId,
            WORKFLOW_PLAN_ID,
            WORKFLOW_STEP_NEW,
            stepId,
            target,
            explanation,
            List.of(step)
        );
    }

    private static String buildStepDisplayName(EntityType entityType, Action action, ExecutionRequest request) {
        return entityType.displayName() + " #" + request.entityId() + ": " + action.displayName();
    }
}
