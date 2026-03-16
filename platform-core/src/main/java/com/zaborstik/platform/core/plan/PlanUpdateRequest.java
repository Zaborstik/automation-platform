package com.zaborstik.platform.core.plan;

import java.util.Objects;

/**
 * DTO для обновления состояния плана.
 */
public record PlanUpdateRequest(
    String planId,
    String newWorkflowStepInternalName,
    String stoppedAtPlanStepId
) {
    public PlanUpdateRequest {
        Objects.requireNonNull(planId, "planId cannot be null");
        Objects.requireNonNull(newWorkflowStepInternalName, "newWorkflowStepInternalName cannot be null");
    }
}
