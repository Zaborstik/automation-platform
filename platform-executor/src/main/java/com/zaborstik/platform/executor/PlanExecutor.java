package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
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
     */
    public PlanExecutionResult execute(Plan plan) {
        Objects.requireNonNull(plan, "plan cannot be null");
        log.info("Executing plan {} for entityType={} entityId={} action={}",
            plan.getId(), plan.getEntityTypeId(), plan.getEntityId(), plan.getActionId());

        Instant startedAt = Instant.now();
        List<ExecutionLogEntry> logEntries = new ArrayList<>();

        List<PlanStep> steps = plan.getSteps();
        List<StepExecutionResult> results = agentService.executePlan(plan);

        int stepsSize = steps.size();
        int resultsSize = results.size();
        int count = Math.min(stepsSize, resultsSize);

        for (int i = 0; i < count; i++) {
            PlanStep step = steps.get(i);
            StepExecutionResult result = results.get(i);
            logEntries.add(new ExecutionLogEntry(
                plan.getId(),
                i,
                step,
                result,
                Instant.now()
            ));
        }

        // Если агент вернул меньше результатов, чем шагов, добавим фиктивные failure-записи.
        if (stepsSize > resultsSize) {
            for (int i = resultsSize; i < stepsSize; i++) {
                PlanStep step = steps.get(i);
                StepExecutionResult syntheticFailure = StepExecutionResult.failure(
                    step.getType(),
                    step.getTarget(),
                    "Step was not executed by agent (no result returned)",
                    0
                );
                logEntries.add(new ExecutionLogEntry(
                    plan.getId(),
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
            plan.getId(),
            success,
            startedAt,
            finishedAt,
            logEntries
        );

        log.info("Plan {} execution finished with status={}, steps={}",
            plan.getId(), success ? "SUCCESS" : "FAILED", logEntries.size());

        return executionResult;
    }
}


