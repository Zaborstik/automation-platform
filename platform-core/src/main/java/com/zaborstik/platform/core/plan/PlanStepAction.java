package com.zaborstik.platform.core.plan;

import java.util.Objects;

/**
 * Действие в рамках шага плана (zbrtstk.plan_step_action).
 * У одного plan_step может быть несколько действий; хранит meta_value (например текст для поиска).
 */
public record PlanStepAction(String actionId, String metaValue) {
    public PlanStepAction(String actionId, String metaValue) {
        this.actionId = Objects.requireNonNull(actionId, "actionId cannot be null");
        this.metaValue = metaValue;
    }
}
