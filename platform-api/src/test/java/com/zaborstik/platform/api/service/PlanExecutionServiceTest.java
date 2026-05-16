package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.PlanRunResponse;
import com.zaborstik.platform.api.dto.StepExecutionReportRequest;
import com.zaborstik.platform.api.entity.AttachmentEntity;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.api.repository.PlanResultRepository;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanExecutionServiceTest {

    @Mock
    private PlanService planService;

    @Mock
    private PlanResultRepository planResultRepository;

    @InjectMocks
    private PlanExecutionService planExecutionService;

    @Test
    void startRunCreatesPlaceholderResultAndTransitionsPlan() {
        Plan plan = simplePlan();
        PlanResultEntity placeholder = new PlanResultEntity();
        placeholder.setId("result-1");

        when(planService.getPlanDomain("plan-1")).thenReturn(Optional.of(plan));
        when(planService.createPlanResult(eq("plan-1"), eq(false), any(Instant.class), any(Instant.class)))
            .thenReturn(placeholder);

        Optional<PlanRunResponse> response = planExecutionService.startRun("plan-1");

        assertTrue(response.isPresent());
        assertEquals("plan-1", response.get().getPlanId());
        assertEquals("result-1", response.get().getPlanResultId());
        verify(planService).transitionPlan("plan-1", "in_progress");
    }

    @Test
    void startRunReturnsEmptyWhenPlanMissing() {
        when(planService.getPlanDomain("missing")).thenReturn(Optional.empty());

        assertTrue(planExecutionService.startRun("missing").isEmpty());
        verify(planService, never()).createPlanResult(any(), anyBoolean(), any(), any());
    }

    @Test
    void reportStepResultPersistsLogOnlyOnFailure() {
        StepExecutionReportRequest report = new StepExecutionReportRequest();
        report.setSuccess(false);
        report.setActionId("act-click");
        report.setMessage("Click submit");
        report.setError("Element not found");
        report.setExecutedAt(Instant.parse("2026-03-15T10:00:05Z"));
        report.setExecutionTimeMs(150L);
        report.setScreenshotPath("/tmp/error.png");

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setId("attachment-1");
        when(planService.createAttachment("/tmp/error.png")).thenReturn(attachment);

        planExecutionService.reportStepResult("plan-1", "step-1", "result-1", report);

        verify(planService).updateStoppedAtPlanStep("plan-1", "step-1");
        verify(planService).transitionPlanStep("plan-1", "step-1", "in_progress");
        verify(planService).transitionPlanStep("plan-1", "step-1", "failed");
        verify(planService).createPlanStepLog(
            eq("plan-1"),
            eq("step-1"),
            eq("result-1"),
            eq("act-click"),
            eq("Click submit"),
            eq("Element not found"),
            any(Instant.class),
            eq(150L),
            eq("attachment-1")
        );
    }

    @Test
    void reportStepResultSkipsLogOnSuccess() {
        StepExecutionReportRequest report = new StepExecutionReportRequest();
        report.setSuccess(true);
        report.setActionId("act-click");
        report.setExecutionTimeMs(100L);

        planExecutionService.reportStepResult("plan-1", "step-1", "result-1", report);

        verify(planService).transitionPlanStep("plan-1", "step-1", "completed");
        verify(planService, never()).createPlanStepLog(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void finishRunUpdatesPlanResultAndTransitionsPlan() {
        Plan plan = simplePlan();
        PlanResultEntity existing = new PlanResultEntity();
        existing.setId("result-1");

        when(planService.getPlanDomain("plan-1")).thenReturn(Optional.of(plan));
        when(planResultRepository.findById("result-1")).thenReturn(Optional.of(existing));

        Instant start = Instant.parse("2026-03-15T10:00:00Z");
        Instant end = Instant.parse("2026-03-15T10:00:10Z");
        Optional<PlanRunResponse> response = planExecutionService.finishRun(
            "plan-1", "result-1", true, 2, 0, start, end);

        assertTrue(response.isPresent());
        assertTrue(response.get().isSuccess());
        verify(planService).transitionPlan("plan-1", "completed");
        verify(planResultRepository).save(existing);
        assertTrue(existing.isSuccess());
        assertEquals(start, existing.getStartedTime());
        assertEquals(end, existing.getFinishedTime());
    }

    @Test
    void finishRunOnFailureTransitionsToFailed() {
        Plan plan = simplePlan();
        when(planService.getPlanDomain("plan-1")).thenReturn(Optional.of(plan));
        when(planResultRepository.findById("result-1")).thenReturn(Optional.empty());

        Optional<PlanRunResponse> response = planExecutionService.finishRun(
            "plan-1", "result-1", false, 2, 1, null, null);

        assertTrue(response.isPresent());
        assertFalse(response.get().isSuccess());
        verify(planService).transitionPlan("plan-1", "failed");
    }

    private static Plan simplePlan() {
        PlanStep step = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-button",
            "#submit",
            0,
            "Click submit",
            List.of(new PlanStepAction("act-click", null))
        );
        return new Plan("plan-1", "wf-plan", "new", "step-1", "target", "explanation", List.of(step));
    }

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }
}
