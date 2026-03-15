package com.zaborstik.platform.api.service;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.api.dto.ExecutePlanResponse;
import com.zaborstik.platform.api.entity.AttachmentEntity;
import com.zaborstik.platform.api.entity.PlanResultEntity;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.executor.ExecutionLogEntry;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanExecutionServiceTest {

    @Mock
    private PlanService planService;

    @Mock
    private PlanExecutor planExecutor;

    @Mock
    private AgentService agentService;

    @InjectMocks
    private PlanExecutionService planExecutionService;

    @Test
    void shouldExecutePlanAndPersistResultLogsAndAttachment() {
        PlanStep step1 = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan",
            "click",
            "ent-button",
            "#submit",
            0,
            "Click submit",
            List.of(new PlanStepAction("act-click", null))
        );
        PlanStep step2 = new PlanStep(
            "step-2",
            "plan-1",
            "wf-plan",
            "type",
            "ent-input",
            "#input",
            1,
            "Type value",
            List.of(new PlanStepAction("act-input-text", "value"))
        );
        Plan plan = new Plan("plan-1", "wf-plan", "new", "step-1", "target", "explanation", List.of(step1, step2));

        StepExecutionResult success = StepExecutionResult.success(
            "step-1",
            "Click submit",
            "ok",
            100,
            "/tmp/ok.png",
            Map.of("x", 120.0, "y", 360.0)
        );
        StepExecutionResult failure = StepExecutionResult.failure(
            "step-2",
            "Type value",
            "Element not found",
            150,
            Map.of("x", 120.0, "y", 390.0, "screenshot", "/tmp/error.png")
        );
        PlanExecutionResult executionResult = new PlanExecutionResult(
            "plan-1",
            false,
            Instant.parse("2026-03-15T10:00:00Z"),
            Instant.parse("2026-03-15T10:00:10Z"),
            List.of(
                new ExecutionLogEntry("plan-1", 0, step1, success, Instant.now()),
                new ExecutionLogEntry("plan-1", 1, step2, failure, Instant.now())
            )
        );

        PlanResultEntity planResult = new PlanResultEntity();
        planResult.setId("result-1");
        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setId("attachment-1");
        attachment.setDisplayname("/tmp/error.png");

        when(planService.getPlanDomain("plan-1")).thenReturn(Optional.of(plan));
        when(planExecutor.execute(plan)).thenReturn(executionResult);
        when(planService.createPlanResult(eq("plan-1"), eq(false), any(Instant.class), any(Instant.class)))
            .thenReturn(planResult);
        when(planService.createAttachment("/tmp/error.png")).thenReturn(attachment);

        Optional<ExecutePlanResponse> response = planExecutionService.executePlan("plan-1");

        assertTrue(response.isPresent());
        assertEquals("plan-1", response.get().getPlanId());
        assertEquals("result-1", response.get().getPlanResultId());
        assertFalse(response.get().isSuccess());
        assertEquals(2, response.get().getTotalSteps());
        assertEquals(1, response.get().getFailedSteps());

        verify(planService).createPlanResult(eq("plan-1"), eq(false), any(Instant.class), any(Instant.class));
        verify(planService).createAttachment("/tmp/error.png");
        verify(planService).createPlanStepLogEntry(
            eq("plan-1"),
            eq("step-2"),
            eq("result-1"),
            eq("act-input-text"),
            eq("Type value"),
            eq("Element not found"),
            any(Instant.class),
            eq(150L),
            eq("attachment-1")
        );
        verify(agentService).close();
    }

    @Test
    void shouldReturnEmptyWhenPlanMissing() {
        when(planService.getPlanDomain("missing")).thenReturn(Optional.empty());

        Optional<ExecutePlanResponse> response = planExecutionService.executePlan("missing");

        assertTrue(response.isEmpty());
        verifyNoInteractions(planExecutor, agentService);
    }
}
