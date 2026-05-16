package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.PlanRunResponse;
import com.zaborstik.platform.api.dto.StepExecutionReportRequest;
import com.zaborstik.platform.api.entity.AttachmentEntity;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.api.repository.PlanResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-side bookkeeping for plan execution.
 *
 * <p>This service no longer drives the browser. The local
 * {@code platform-executor} fetches a plan over HTTP, runs steps on the
 * user's machine and then reports outcomes back through this service.
 * Lifecycle transitions, {@code plan_result} aggregation and
 * {@code plan_step_log} persistence all happen here.
 */
@Service
public class PlanExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutionService.class);

    private static final int ERROR_MAX_LEN = 2000;

    private final PlanService planService;
    private final PlanResultRepository planResultRepository;

    public PlanExecutionService(PlanService planService, PlanResultRepository planResultRepository) {
        this.planService = planService;
        this.planResultRepository = planResultRepository;
    }

    /**
     * Marks the beginning of an external (local) run. Transitions the plan to
     * {@code in_progress} and pre-creates an empty {@code plan_result} record
     * that subsequent step reports will be attached to.
     */
    @Transactional
    public Optional<PlanRunResponse> startRun(String planId) {
        Objects.requireNonNull(planId, "planId");
        if (planService.getPlanDomain(planId).isEmpty()) {
            return Optional.empty();
        }
        safelyTransitionPlan(planId, "in_progress");

        Instant now = Instant.now();
        PlanResultEntity placeholder = planService.createPlanResult(planId, false, now, now);

        PlanRunResponse response = new PlanRunResponse();
        response.setPlanId(planId);
        response.setPlanResultId(placeholder.getId());
        response.setStartedTime(now);
        return Optional.of(response);
    }

    /**
     * Persists the outcome of a single plan step reported by the local executor
     * and transitions the step lifecycle accordingly.
     */
    @Transactional
    public void reportStepResult(String planId, String planStepId, String planResultId,
                                 StepExecutionReportRequest report) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(planStepId, "planStepId");
        Objects.requireNonNull(report, "report");

        safelyUpdateStoppedAt(planId, planStepId);
        safelyTransitionPlanStep(planId, planStepId, "in_progress");

        boolean success = Boolean.TRUE.equals(report.getSuccess());
        safelyTransitionPlanStep(planId, planStepId, success ? "completed" : "failed");

        if (success || planResultId == null || planResultId.isBlank()) {
            return;
        }
        String actionId = report.getActionId();
        if (actionId == null || actionId.isBlank()) {
            log.warn("Skipping plan_step_log for plan {} step {} because actionId is missing",
                planId, planStepId);
            return;
        }
        String attachmentId = null;
        String screenshotPath = report.getScreenshotPath();
        if (screenshotPath != null && !screenshotPath.isBlank()) {
            AttachmentEntity attachment = planService.createAttachment(screenshotPath);
            attachmentId = attachment.getId();
        }
        Instant executedAt = report.getExecutedAt() != null ? report.getExecutedAt() : Instant.now();
        planService.createPlanStepLog(
            planId,
            planStepId,
            planResultId,
            actionId,
            report.getMessage() != null ? report.getMessage() : planStepId,
            truncate(report.getError(), ERROR_MAX_LEN),
            executedAt,
            report.getExecutionTimeMs() != null ? report.getExecutionTimeMs() : 0L,
            attachmentId
        );
    }

    /**
     * Finalises a run reported by the local executor: transitions the plan to
     * {@code completed} or {@code failed} and refreshes the aggregated
     * {@code plan_result} record created in {@link #startRun(String)}.
     */
    @Transactional
    public Optional<PlanRunResponse> finishRun(String planId, String planResultId,
                                               boolean success, int totalSteps, int failedSteps,
                                               Instant startedAt, Instant finishedAt) {
        Objects.requireNonNull(planId, "planId");
        if (planService.getPlanDomain(planId).isEmpty()) {
            return Optional.empty();
        }
        safelyTransitionPlan(planId, success ? "completed" : "failed");

        Instant now = Instant.now();
        Instant effectiveFinished = finishedAt != null ? finishedAt : now;
        Instant effectiveStarted = startedAt != null ? startedAt : effectiveFinished;

        if (planResultId != null && !planResultId.isBlank()) {
            try {
                PlanResultEntity result = planResultRepository.findById(planResultId)
                    .orElseThrow(() -> new NoSuchElementException("Plan result not found: " + planResultId));
                result.setSuccess(success);
                result.setStartedTime(effectiveStarted);
                result.setFinishedTime(effectiveFinished);
                planResultRepository.save(result);
            } catch (Exception ex) {
                log.warn("Failed to update plan_result {} for plan {}", planResultId, planId, ex);
            }
        }

        PlanRunResponse response = new PlanRunResponse();
        response.setPlanId(planId);
        response.setPlanResultId(planResultId);
        response.setSuccess(success);
        response.setTotalSteps(totalSteps);
        response.setFailedSteps(failedSteps);
        response.setStartedTime(effectiveStarted);
        response.setFinishedTime(effectiveFinished);
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

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        if (value.length() <= maxLen) return value;
        return value.substring(0, maxLen);
    }
}
