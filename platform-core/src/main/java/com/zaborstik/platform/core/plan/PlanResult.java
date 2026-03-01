package com.zaborstik.platform.core.plan;

import java.time.Instant;
import java.util.Objects;

/**
 * Итог выполнения плана (zbrtstk.plan_result).
 */
public record PlanResult(
    String id,
    String planId,
    boolean success,
    Instant startedTime,
    Instant finishedTime
) {
    public PlanResult(String id, String planId, boolean success, Instant startedTime, Instant finishedTime) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.planId = Objects.requireNonNull(planId, "planId cannot be null");
        this.success = success;
        this.startedTime = Objects.requireNonNull(startedTime, "startedTime cannot be null");
        this.finishedTime = Objects.requireNonNull(finishedTime, "finishedTime cannot be null");
    }
}
