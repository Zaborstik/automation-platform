package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Высокоуровневый результат исполнения плана.
 * Содержит агрегированный статус, временные метки и execution_log.
 */
public record PlanExecutionResult(String planId, boolean success, Instant startedAt, Instant finishedAt,
                                  List<ExecutionLogEntry> logEntries) {
    public PlanExecutionResult(String planId,
                               boolean success,
                               Instant startedAt,
                               Instant finishedAt,
                               List<ExecutionLogEntry> logEntries) {
        this.planId = Objects.requireNonNull(planId, "planId cannot be null");
        this.success = success;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.logEntries = logEntries != null
                ? List.copyOf(logEntries)
                : List.of();
    }

    /**
     * Удобный метод для получения «сырых» результатов выполнения шагов.
     */
    public List<StepExecutionResult> getStepResults() {
        return logEntries.stream()
                .map(ExecutionLogEntry::result)
                .toList();
    }

    @Override
    public String toString() {
        return "PlanExecutionResult{" +
                "planId='" + planId + '\'' +
                ", success=" + success +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", steps=" + logEntries.size() +
                '}';
    }
}


