package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.agent.service.StepExecutionCallback;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanExecutorTest {

    @Mock
    private AgentService agentService;

    @Mock
    private StepExecutionCallback callback;

    private PlanExecutor executor;
    private Plan testPlan;

    @BeforeEach
    void setUp() {
        executor = new PlanExecutor(agentService);
        testPlan = new Plan(
            "plan-1",
            "wf-plan",
            "in_progress",
            "step-1",
            "Building 93939",
            "Test plan",
            List.of(
                step("step-1", 0, "Step 1"),
                step("step-2", 1, "Step 2"),
                step("step-3", 2, "Step 3"),
                step("step-4", 3, "Step 4")
            )
        );
    }

    @Test
    void stopOnFailureTrueShouldLeaveRemainingStepsUnexecuted() {
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("s1", "t1", "ok", 10, null),
            StepExecutionResult.failure("s2", "t2", "err", 10)
        );
        when(agentService.executePlan(any(Plan.class), anyBoolean(), any(StepExecutionCallback.class)))
            .thenReturn(results);

        PlanExecutionResult executionResult = executor.execute(testPlan, true);

        assertFalse(executionResult.success());
        assertEquals(4, executionResult.logEntries().size());
        assertTrue(executionResult.logEntries().get(0).result().success());
        assertFalse(executionResult.logEntries().get(1).result().success());
        assertTrue(executionResult.logEntries().get(2).result().error().contains("not executed"));
        assertTrue(executionResult.logEntries().get(3).result().error().contains("not executed"));
    }

    @Test
    void stopOnFailureFalseShouldProcessAllReturnedResults() {
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("s1", "t1", "ok", 10, null),
            StepExecutionResult.failure("s2", "t2", "err", 10),
            StepExecutionResult.success("s3", "t3", "ok", 10, null),
            StepExecutionResult.success("s4", "t4", "ok", 10, null)
        );
        when(agentService.executePlan(any(Plan.class), anyBoolean(), any(StepExecutionCallback.class)))
            .thenReturn(results);

        PlanExecutionResult executionResult = executor.execute(testPlan, false);

        assertEquals(4, executionResult.logEntries().size());
        assertFalse(executionResult.logEntries().get(1).result().success());
        assertTrue(executionResult.logEntries().get(2).result().success());
        assertTrue(executionResult.logEntries().get(3).result().success());
    }

    @Test
    void executeWithoutFlagShouldRemainBackwardCompatible() {
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("s1", "t1", "ok", 10, null),
            StepExecutionResult.success("s2", "t2", "ok", 10, null),
            StepExecutionResult.success("s3", "t3", "ok", 10, null),
            StepExecutionResult.success("s4", "t4", "ok", 10, null)
        );
        when(agentService.executePlan(any(Plan.class), anyBoolean(), any(StepExecutionCallback.class)))
            .thenReturn(results);

        PlanExecutionResult executionResult = executor.execute(testPlan);

        assertTrue(executionResult.success());
        verify(agentService).executePlan(any(Plan.class), org.mockito.ArgumentMatchers.eq(false), any(StepExecutionCallback.class));
    }

    @Test
    void executeWithCallbackShouldDelegateCallbackToAgentService() {
        when(agentService.executePlan(any(Plan.class), anyBoolean(), any(StepExecutionCallback.class)))
            .thenReturn(List.of());

        executor.execute(testPlan, false, callback);

        verify(agentService).executePlan(testPlan, false, callback);
    }

    @Test
    void shouldThrowExceptionWhenPlanIsNull() {
        assertThrows(NullPointerException.class, () -> executor.execute(null));
    }

    @Test
    void shouldThrowExceptionWhenAgentServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new PlanExecutor(null));
    }

    private static PlanStep step(String id, int sortOrder, String displayName) {
        return new PlanStep(
            id,
            "plan-1",
            "workflow-1",
            "in_progress",
            "ent-page",
            "93939",
            sortOrder,
            displayName,
            List.of()
        );
    }
}
