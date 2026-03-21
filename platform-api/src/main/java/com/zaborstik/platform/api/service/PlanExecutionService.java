package com.zaborstik.platform.api.service;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.api.dto.ExecutePlanResponse;
import com.zaborstik.platform.api.entity.AttachmentEntity;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.executor.ExecutionLogEntry;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Оркестрация выполнения плана через browser executor и сохранение итогов в БД.
 */
@Service
public class PlanExecutionService {
    private static final Logger log = LoggerFactory.getLogger(PlanExecutionService.class);

    private final PlanService planService;
    private final PlanExecutor planExecutor;

    public PlanExecutionService(PlanService planService,
                                PlanExecutor planExecutor) {
        this.planService = planService;
        this.planExecutor = planExecutor;
    }

    public Optional<ExecutePlanResponse> executePlan(String planId) {
        Optional<Plan> maybePlan = planService.getPlanDomain(planId);
        if (maybePlan.isEmpty()) {
            return Optional.empty();
        }

        Plan plan = maybePlan.get();
        safelyTransitionPlan(plan.id(), "in_progress");
        PlanExecutionResult executionResult = planExecutor.execute(plan);

        PlanResultEntity planResult = planService.createPlanResult(
            executionResult.planId(),
            executionResult.success(),
            executionResult.startedAt(),
            executionResult.finishedAt()
        );

        int failedSteps = 0;
        for (ExecutionLogEntry logEntry : executionResult.logEntries()) {
            safelyUpdateStoppedAt(executionResult.planId(), logEntry.step().id());
            safelyTransitionPlanStep(executionResult.planId(), logEntry.step().id(), "in_progress");

            StepExecutionResult stepResult = logEntry.result();
            if (stepResult.success()) {
                safelyTransitionPlanStep(executionResult.planId(), logEntry.step().id(), "completed");
                continue;
            }
            safelyTransitionPlanStep(executionResult.planId(), logEntry.step().id(), "failed");
            failedSteps++;
            String actionId = resolveActionId(logEntry);
            if (actionId == null) {
                log.warn("Skipping log persistence for step {} because actionId is missing", logEntry.step().id());
                continue;
            }

            String screenshotPath = resolveScreenshotPath(stepResult);
            String attachmentId = null;
            if (screenshotPath != null && !screenshotPath.isBlank()) {
                AttachmentEntity attachment = planService.createAttachment(screenshotPath);
                attachmentId = attachment.getId();
            }

            String message = stepResult.message() != null ? stepResult.message() : logEntry.step().displayName();
            String error = truncateForDb(stepResult.error(), 2000);
            planService.createPlanStepLog(
                executionResult.planId(),
                logEntry.step().id(),
                planResult.getId(),
                actionId,
                message,
                error,
                stepResult.executedAt(),
                stepResult.executionTimeMs(),
                attachmentId
            );
        }

        safelyTransitionPlan(executionResult.planId(), executionResult.success() ? "completed" : "failed");

        ExecutePlanResponse response = new ExecutePlanResponse();
        response.setPlanId(executionResult.planId());
        response.setPlanResultId(planResult.getId());
        response.setSuccess(executionResult.success());
        response.setTotalSteps(executionResult.logEntries().size());
        response.setFailedSteps(failedSteps);
        response.setStartedTime(executionResult.startedAt());
        response.setFinishedTime(executionResult.finishedAt());
        return Optional.of(response);
    }

    private void safelyTransitionPlan(String planId, String targetStep) {
        try {
            planService.transitionPlan(planId, targetStep);
        } catch (Exception ex) {
            log.warn("Failed to transition plan {} to {}", planId, targetStep, ex);
        }
    }

    private void safelyTransitionPlanStep(String planId, String stepId, String targetStep) {
        try {
            planService.transitionPlanStep(planId, stepId, targetStep);
        } catch (Exception ex) {
            log.warn("Failed to transition plan step {}:{} to {}", planId, stepId, targetStep, ex);
        }
    }

    private void safelyUpdateStoppedAt(String planId, String stepId) {
        try {
            planService.updateStoppedAtPlanStep(planId, stepId);
        } catch (Exception ex) {
            log.warn("Failed to update stoppedAtPlanStep for plan {} and step {}", planId, stepId, ex);
        }
    }

    private String resolveActionId(ExecutionLogEntry logEntry) {
        return logEntry.step().actions().stream()
            .map(PlanStepAction::actionId)
            .filter(actionId -> actionId != null && !actionId.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String resolveScreenshotPath(StepExecutionResult stepResult) {
        if (stepResult.screenshotPath() != null && !stepResult.screenshotPath().isBlank()) {
            return stepResult.screenshotPath();
        }
        Map<String, Object> metadata = stepResult.metadata();
        Object screenshot = metadata.get("screenshot");
        return screenshot instanceof String screenshotPath ? screenshotPath : null;
    }

    private static String truncateForDb(String value, int maxLen) {
        if (value == null) return null;
        if (value.length() <= maxLen) return value;
        return value.substring(0, maxLen);
    }
}
