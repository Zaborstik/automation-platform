package com.zaborstik.platform.api.service;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
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
    private final AgentService agentService;

    public PlanExecutionService(PlanService planService,
                                PlanExecutor planExecutor,
                                AgentService agentService) {
        this.planService = planService;
        this.planExecutor = planExecutor;
        this.agentService = agentService;
    }

    public Optional<ExecutePlanResponse> executePlan(String planId) {
        Optional<Plan> maybePlan = planService.getPlanDomain(planId);
        if (maybePlan.isEmpty()) {
            return Optional.empty();
        }

        Plan plan = maybePlan.get();
        PlanExecutionResult executionResult = planExecutor.execute(plan);

        PlanResultEntity planResult = planService.createPlanResult(
            executionResult.getPlanId(),
            executionResult.isSuccess(),
            executionResult.getStartedAt(),
            executionResult.getFinishedAt()
        );

        int failedSteps = 0;
        for (ExecutionLogEntry logEntry : executionResult.getLogEntries()) {
            StepExecutionResult stepResult = logEntry.getResult();
            if (stepResult.isSuccess()) {
                continue;
            }
            failedSteps++;
            String actionId = resolveActionId(logEntry);
            if (actionId == null) {
                log.warn("Skipping log persistence for step {} because actionId is missing", logEntry.getStep().id());
                continue;
            }

            String screenshotPath = resolveScreenshotPath(stepResult);
            String attachmentId = null;
            if (screenshotPath != null && !screenshotPath.isBlank()) {
                AttachmentEntity attachment = planService.createAttachment(screenshotPath);
                attachmentId = attachment.getId();
            }

            String message = stepResult.getMessage() != null ? stepResult.getMessage() : logEntry.getStep().displayName();
            planService.createPlanStepLogEntry(
                executionResult.getPlanId(),
                logEntry.getStep().id(),
                planResult.getId(),
                actionId,
                message,
                stepResult.getError(),
                stepResult.getExecutedAt(),
                stepResult.getExecutionTimeMs(),
                attachmentId
            );
        }

        ExecutePlanResponse response = new ExecutePlanResponse();
        response.setPlanId(executionResult.getPlanId());
        response.setPlanResultId(planResult.getId());
        response.setSuccess(executionResult.isSuccess());
        response.setTotalSteps(executionResult.getLogEntries().size());
        response.setFailedSteps(failedSteps);
        response.setStartedTime(executionResult.getStartedAt());
        response.setFinishedTime(executionResult.getFinishedAt());
        return Optional.of(response);
    }

    private String resolveActionId(ExecutionLogEntry logEntry) {
        return logEntry.getStep().actions().stream()
            .map(PlanStepAction::actionId)
            .filter(actionId -> actionId != null && !actionId.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String resolveScreenshotPath(StepExecutionResult stepResult) {
        if (stepResult.getScreenshotPath() != null && !stepResult.getScreenshotPath().isBlank()) {
            return stepResult.getScreenshotPath();
        }
        Map<String, Object> metadata = stepResult.getMetadata();
        Object screenshot = metadata.get("screenshot");
        return screenshot instanceof String screenshotPath ? screenshotPath : null;
    }
}
