package com.zaborstik.platform.core.plan;

import java.time.Instant;
import java.util.Objects;

/**
 * Лог по шагу выполнения (zbrtstk.plan_step_log_entry).
 * Создаётся при падении/прерывании; может содержать скриншот (attachment).
 */
public record PlanStepLogEntry(
    String id,
    String planId,
    String planStepId,
    String planResultId,
    String actionId,
    String message,
    String error,
    Instant executedTime,
    Long executionTimeMs,
    String attachmentId
) {
    public PlanStepLogEntry(String id, String planId, String planStepId, String planResultId,
                           String actionId, String message, String error, Instant executedTime,
                           Long executionTimeMs, String attachmentId) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.planId = Objects.requireNonNull(planId, "planId cannot be null");
        this.planStepId = Objects.requireNonNull(planStepId, "planStepId cannot be null");
        this.planResultId = Objects.requireNonNull(planResultId, "planResultId cannot be null");
        this.actionId = Objects.requireNonNull(actionId, "actionId cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.error = error;
        this.executedTime = Objects.requireNonNull(executedTime, "executedTime cannot be null");
        this.executionTimeMs = executionTimeMs;
        this.attachmentId = attachmentId;
    }
}
