package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.agent.service.StepExecutionCallback;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Исполнитель планов.
 *
 * Берёт {@link Plan}, передаёт шаги в {@link AgentService}, собирает execution_log
 * и возвращает агрегированный {@link PlanExecutionResult}.
 * 
 * Plan executor.
 * 
 * Takes {@link Plan}, passes steps to {@link AgentService}, collects execution_log
 * and returns aggregated {@link PlanExecutionResult}.
 */
public class PlanExecutor {
    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final AgentService agentService;

    public PlanExecutor(AgentService agentService) {
        this.agentService = Objects.requireNonNull(agentService, "agentService cannot be null");
    }

    /**
     * Синхронно выполняет план.
     *
     * @param plan план для исполнения
     * @return результат исполнения с execution_log
     * 
     * Synchronously executes plan.
     * 
     * @param plan plan to execute
     * @return execution result with execution_log
     */
    public PlanExecutionResult execute(Plan plan) {
        return execute(plan, false, StepExecutionCallback.noOp());
    }

    public PlanExecutionResult execute(Plan plan, boolean stopOnFailure) {
        return execute(plan, stopOnFailure, StepExecutionCallback.noOp());
    }

    public PlanExecutionResult execute(Plan plan, boolean stopOnFailure, StepExecutionCallback callback) {
        Objects.requireNonNull(plan, "plan cannot be null");
        StepExecutionCallback effectiveCallback = callback != null ? callback : StepExecutionCallback.noOp();
        log.info("Executing plan {} target={}",
            plan.id(), plan.target());

        Instant startedAt = Instant.now();
        List<ExecutionLogEntry> logEntries = new ArrayList<>();

        List<PlanStep> steps = plan.steps();
        List<StepExecutionResult> results = agentService.executePlan(plan, stopOnFailure, effectiveCallback);

        int stepsSize = steps.size();
        int resultsSize = results.size();
        int count = Math.min(stepsSize, resultsSize);
        int processedCount = count;

        if (stopOnFailure) {
            for (int i = 0; i < count; i++) {
                if (!results.get(i).isSuccess()) {
                    processedCount = i + 1;
                    break;
                }
            }
        }

        for (int i = 0; i < processedCount; i++) {
            PlanStep step = steps.get(i);
            StepExecutionResult result = results.get(i);
            logEntries.add(new ExecutionLogEntry(
                plan.id(),
                i,
                step,
                result,
                Instant.now()
            ));
        }

        // Если агент вернул меньше результатов, чем шагов, добавим фиктивные failure-записи.
        // If agent returned fewer results than steps, add synthetic failure entries.
        if (stepsSize > processedCount) {
            for (int i = processedCount; i < stepsSize; i++) {
                PlanStep step = steps.get(i);
                StepExecutionResult syntheticFailure = StepExecutionResult.failure(
                    step.id(),
                    step.displayName(),
                    "Step was not executed by agent (no result returned)",
                    0,
                    java.util.Map.of(),
                    0,
                    i,
                    null
                );
                logEntries.add(new ExecutionLogEntry(
                    plan.id(),
                    i,
                    step,
                    syntheticFailure,
                    Instant.now()
                ));
            }
        }

        boolean success = logEntries.stream().allMatch(e -> e.getResult().isSuccess());
        Instant finishedAt = Instant.now();

        PlanExecutionResult executionResult = new PlanExecutionResult(
            plan.id(),
            success,
            startedAt,
            finishedAt,
            logEntries
        );

        log.info("Plan {} execution finished with status={}, steps={}",
            plan.id(), success ? "SUCCESS" : "FAILED", logEntries.size());

        return executionResult;
    }
}


