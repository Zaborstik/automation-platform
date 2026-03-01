package com.zaborstik.platform.core.plan;

import java.util.List;
import java.util.Objects;

/**
 * Шаг плана (zbrtstk.plan_step).
 * Мини-задача: имеет свой ЖЦ, тип сущности, объект действия и список действий (plan_step_action).
 */
public record PlanStep(
    String id,
    String planId,
    String workflowId,
    String workflowStepInternalName,
    String entityTypeId,
    String entityId,
    int sortOrder,
    String displayName,
    List<PlanStepAction> actions
) {
    public PlanStep(String id, String planId, String workflowId, String workflowStepInternalName,
                    String entityTypeId, String entityId, int sortOrder, String displayName,
                    List<PlanStepAction> actions) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.planId = Objects.requireNonNull(planId, "planId cannot be null");
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId cannot be null");
        this.workflowStepInternalName = Objects.requireNonNull(workflowStepInternalName, "workflowStepInternalName cannot be null");
        this.entityTypeId = Objects.requireNonNull(entityTypeId, "entityTypeId cannot be null");
        this.entityId = entityId;
        this.sortOrder = sortOrder;
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.actions = actions != null ? List.copyOf(actions) : List.of();
    }
}
