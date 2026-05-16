package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.executor.client.AgentRestClient;
import com.zaborstik.platform.executor.client.RemoteApiClient;
import com.zaborstik.platform.executor.client.api.PlanRunStartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates plan execution on the local machine.
 *
 * <ol>
 *   <li>Asks {@code platform-api} to start a run for the plan.</li>
 *   <li>For each step: resolves the action's {@code internalname} via
 *       {@code RemoteResolver}, calls the local {@code platform-agent} to
 *       perform the operation, then posts the result back to the api.</li>
 *   <li>Finalises the run with success/failure summary.</li>
 * </ol>
 *
 * <p>This class replaces the old in-process executor that called
 * {@code AgentService} directly. All inter-service communication is now HTTP.
 */
@Component
public class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final RemoteApiClient apiClient;
    private final AgentRestClient agentClient;
    private final Resolver resolver;

    public PlanExecutor(RemoteApiClient apiClient, AgentRestClient agentClient, Resolver resolver) {
        this.apiClient = apiClient;
        this.agentClient = agentClient;
        this.resolver = resolver;
    }

    public PlanExecutionResult execute(Plan plan, String baseUrl, boolean headless) {
        Instant startedAt = Instant.now();
        List<ExecutionLogEntry> logEntries = new ArrayList<>();
        boolean success = true;

        PlanRunStartDto run = apiClient.startRun(plan.id());
        log.info("Started run for plan {}: planResultId={}", plan.id(), run.planResultId);

        try {
            agentClient.initializeSession(baseUrl, headless);
        } catch (Exception ex) {
            log.error("Failed to initialise local agent: {}", ex.getMessage(), ex);
            success = false;
            return finishRun(plan, run, startedAt, logEntries, false, true);
        }

        try {
            for (int i = 0; i < plan.steps().size(); i++) {
                PlanStep step = plan.steps().get(i);
                StepOutcome outcome = executeOneStep(step, i);
                logEntries.add(new ExecutionLogEntry(plan.id(), i, step, outcome.result, Instant.now()));

                reportStepBack(plan, run, step, outcome);

                if (!outcome.result.success()) {
                    success = false;
                }
            }
        } finally {
            try {
                agentClient.closeSession();
            } catch (Exception ex) {
                log.warn("Failed to close local agent: {}", ex.getMessage());
            }
        }

        return finishRun(plan, run, startedAt, logEntries, success, false);
    }

    private PlanExecutionResult finishRun(Plan plan, PlanRunStartDto run, Instant startedAt,
                                          List<ExecutionLogEntry> logEntries, boolean success,
                                          boolean abortedBeforeSteps) {
        Instant finishedAt = Instant.now();
        int failed = (int) logEntries.stream().filter(e -> !e.result().success()).count();
        if (abortedBeforeSteps) {
            failed = 0;
        }
        try {
            apiClient.finishRun(plan.id(), run.planResultId, success,
                logEntries.size(), failed, startedAt, finishedAt);
        } catch (Exception ex) {
            log.warn("Failed to finish run for plan {}: {}", plan.id(), ex.getMessage());
        }
        return new PlanExecutionResult(plan.id(), success, startedAt, finishedAt, logEntries);
    }

    private StepOutcome executeOneStep(PlanStep step, int index) {
        String operation = resolveOperation(step).orElse(null);
        if (operation == null) {
            StepExecutionResult failure = StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Cannot resolve operation: no plan_step_action with a known actionId",
                0L,
                Map.of(),
                0,
                index,
                null
            );
            return new StepOutcome(failure, null);
        }
        Map<String, String> selectors = collectSelectors(step);
        StepExecutionResult result;
        try {
            result = agentClient.executeStep(step, operation, selectors, index);
        } catch (Exception ex) {
            log.error("Agent failed to execute step {}: {}", step.id(), ex.getMessage(), ex);
            result = StepExecutionResult.failure(
                step.id(),
                step.displayName(),
                "Agent call failed: " + ex.getMessage(),
                0L,
                Map.of(),
                0,
                index,
                null
            );
        }
        String actionId = step.actions().stream()
            .map(PlanStepAction::actionId)
            .filter(a -> a != null && !a.isBlank())
            .findFirst()
            .orElse(null);
        return new StepOutcome(result, actionId);
    }

    private Optional<String> resolveOperation(PlanStep step) {
        for (PlanStepAction a : step.actions()) {
            if (a.actionId() == null || a.actionId().isBlank()) {
                continue;
            }
            Optional<Action> action = resolver.findAction(a.actionId());
            if (action.isPresent()) {
                return Optional.of(action.get().internalName());
            }
        }
        String fallback = step.workflowStepInternalName();
        if (fallback != null && !resolver.isWorkflowStepInternalName(fallback)) {
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    private Map<String, String> collectSelectors(PlanStep step) {
        Map<String, String> selectors = new LinkedHashMap<>();
        for (PlanStepAction a : step.actions()) {
            if (a.actionId() != null) {
                resolver.findUIBinding(a.actionId())
                    .ifPresent(binding -> selectors.put(a.actionId(), binding.selector()));
            }
        }
        return selectors;
    }

    private void reportStepBack(Plan plan, PlanRunStartDto run, PlanStep step, StepOutcome outcome) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", outcome.result.success());
        body.put("message", outcome.result.message());
        body.put("error", outcome.result.error());
        body.put("screenshotPath", outcome.result.screenshotPath());
        body.put("executionTimeMs", outcome.result.executionTimeMs());
        body.put("executedAt", outcome.result.executedAt());
        body.put("metadata", outcome.result.metadata());
        if (outcome.actionId != null) {
            body.put("actionId", outcome.actionId);
        }
        try {
            apiClient.reportStepResult(plan.id(), step.id(), run.planResultId, body);
        } catch (Exception ex) {
            log.warn("Failed to report step {} for plan {}: {}",
                step.id(), plan.id(), ex.getMessage());
        }
    }

    private record StepOutcome(StepExecutionResult result, String actionId) {
    }
}
