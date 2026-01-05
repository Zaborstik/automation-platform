package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;

import java.time.Instant;
import java.util.Objects;

/**
 * Одна запись в execution_log.
 * Связывает шаг плана с фактическим результатом выполнения через UI-агента.
 */
public class ExecutionLogEntry {
    private final String planId;
    private final int stepIndex;
    private final PlanStep step;
    private final StepExecutionResult result;
    private final Instant loggedAt;

    public ExecutionLogEntry(String planId, int stepIndex, PlanStep step, StepExecutionResult result, Instant loggedAt) {
        this.planId = Objects.requireNonNull(planId, "planId cannot be null");
        this.stepIndex = stepIndex;
        this.step = Objects.requireNonNull(step, "step cannot be null");
        this.result = Objects.requireNonNull(result, "result cannot be null");
        this.loggedAt = loggedAt != null ? loggedAt : Instant.now();
    }

    public String getPlanId() {
        return planId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public PlanStep getStep() {
        return step;
    }

    public StepExecutionResult getResult() {
        return result;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    @Override
    public String toString() {
        return "ExecutionLogEntry{" +
            "planId='" + planId + '\'' +
            ", stepIndex=" + stepIndex +
            ", step=" + step +
            ", result=" + result +
            ", loggedAt=" + loggedAt +
            '}';
    }
}


