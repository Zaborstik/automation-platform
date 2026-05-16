package com.zaborstik.platform.executor.client.api;

import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;

import java.util.List;

/**
 * Converts {@link PlanDto} responses from {@code platform-api} into the
 * canonical {@link Plan} model used by the executor's {@code PlanExecutor}.
 */
public final class PlanDtoMapper {

    private PlanDtoMapper() {
    }

    public static Plan toDomain(PlanDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("PlanDto cannot be null");
        }
        List<PlanStep> steps = dto.steps != null
            ? dto.steps.stream().map(s -> toStep(dto.id, s)).toList()
            : List.of();
        return new Plan(
            dto.id,
            dto.workflowId,
            nonBlank(dto.workflowStepInternalName, "new"),
            nonBlank(dto.stoppedAtPlanStepId, dto.id),
            dto.target,
            dto.explanation,
            steps
        );
    }

    private static PlanStep toStep(String planId, PlanDto.PlanStepDto src) {
        List<PlanStepAction> actions = src.actions != null
            ? src.actions.stream()
                .map(a -> new PlanStepAction(a.actionId, a.metaValue))
                .toList()
            : List.of();
        return new PlanStep(
            src.id,
            planId,
            src.workflowId,
            nonBlank(src.workflowStepInternalName, "new"),
            src.entityTypeId,
            src.entityId,
            src.sortOrder,
            nonBlank(src.displayName, src.id),
            actions
        );
    }

    private static String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
