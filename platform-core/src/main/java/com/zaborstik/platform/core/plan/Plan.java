package com.zaborstik.platform.core.plan;

import java.util.List;
import java.util.Objects;

/**
 * План выполнения (zbrtstk.plan).
 * Имеет ЖЦ (workflow + workflowStepInternalName), хранит шаг, на котором остановилось выполнение (stoppedAtPlanStepId).
 */
public record Plan(
    String id,
    String workflowId,
    String workflowStepInternalName,
    String stoppedAtPlanStepId,
    String target,
    String explanation,
    List<PlanStep> steps
) {
    public Plan(String id, String workflowId, String workflowStepInternalName, String stoppedAtPlanStepId,
                String target, String explanation, List<PlanStep> steps) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId cannot be null");
        this.workflowStepInternalName = Objects.requireNonNull(workflowStepInternalName, "workflowStepInternalName cannot be null");
        this.stoppedAtPlanStepId = Objects.requireNonNull(stoppedAtPlanStepId, "stoppedAtPlanStepId cannot be null");
        this.target = target;
        this.explanation = explanation;
        this.steps = steps != null ? List.copyOf(steps) : List.of();
    }
}
