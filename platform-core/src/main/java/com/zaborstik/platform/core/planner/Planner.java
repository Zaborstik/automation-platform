package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;

import java.util.List;
import java.util.Objects;
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
        Objects.requireNonNull(request, "request cannot be null");
        EntityType entityType = resolveEntityType(request.entityType());
        Action action = resolveAction(request.action());
        validateApplicable(action.id(), entityType.id());

        String planId = UUID.randomUUID().toString();
        String stepId = UUID.randomUUID().toString();

        PlanStep step = buildStep(planId, stepId, request, entityType, action, 1);

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

    /**
     * Создаёт многошаговый план из последовательности запросов.
     */
    public Plan createMultiStepPlan(String target, String explanation, List<ExecutionRequest> requests) {
        Objects.requireNonNull(requests, "requests cannot be null");
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests cannot be empty");
        }

        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new java.util.ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            ExecutionRequest request = Objects.requireNonNull(requests.get(i), "request cannot be null");
            EntityType entityType = resolveEntityType(request.entityType());
            Action action = resolveAction(request.action());
            validateApplicable(action.id(), entityType.id());

            String stepId = UUID.randomUUID().toString();
            steps.add(buildStep(planId, stepId, request, entityType, action, i + 1));
        }

        return new Plan(
            planId,
            WORKFLOW_PLAN_ID,
            WORKFLOW_STEP_NEW,
            steps.get(0).id(),
            target,
            explanation,
            steps
        );
    }

    private EntityType resolveEntityType(String entityTypeId) {
        return resolver.findEntityType(entityTypeId)
            .orElseThrow(() -> new IllegalArgumentException("EntityType not found: " + entityTypeId));
    }

    private Action resolveAction(String actionId) {
        return resolver.findAction(actionId)
            .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));
    }

    private void validateApplicable(String actionId, String entityTypeId) {
        if (!resolver.isActionApplicable(actionId, entityTypeId)) {
            throw new IllegalArgumentException(
                "Action '" + actionId + "' is not applicable to entity type '" + entityTypeId + "'"
            );
        }
    }

    private PlanStep buildStep(
        String planId,
        String stepId,
        ExecutionRequest request,
        EntityType entityType,
        Action action,
        int sortOrder
    ) {
        String metaValue = request.parameters().get("meta_value") != null
            ? String.valueOf(request.parameters().get("meta_value"))
            : null;

        return new PlanStep(
            stepId,
            planId,
            WORKFLOW_PLAN_STEP_ID,
            WORKFLOW_STEP_NEW,
            entityType.id(),
            request.entityId(),
            sortOrder,
            buildStepDisplayName(entityType, action, request),
            List.of(new PlanStepAction(action.id(), metaValue))
        );
    }

    private static String buildStepDisplayName(EntityType entityType, Action action, ExecutionRequest request) {
        return entityType.displayName() + " #" + request.entityId() + ": " + action.displayName();
    }
}
