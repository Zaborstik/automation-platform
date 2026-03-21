package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepExecutionCallbackTest {

    @Mock
    private AgentClient agentClient;

    @Mock
    private Resolver resolver;

    @Test
    void callbackShouldBeCalledInExpectedOrder() throws Exception {
        AgentService service = new AgentService(
            agentClient,
            resolver,
            "https://example.org",
            true,
            RetryPolicy.noRetry()
        );

        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("ok", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(
                AgentResponse.success("s1", java.util.Map.of(), 1),
                AgentResponse.success("s2", java.util.Map.of(), 1)
            );

        Plan plan = planWithTwoSteps();
        RecordingCallback callback = new RecordingCallback();

        service.executePlan(plan, callback);

        assertEquals(
            List.of(
                "planStarted",
                "stepStarted:0",
                "stepCompleted:0:true",
                "stepStarted:1",
                "stepCompleted:1:true",
                "planCompleted:true"
            ),
            callback.events
        );
    }

    @Test
    void callbackShouldStillBeCalledWhenStepFails() throws Exception {
        AgentService service = new AgentService(
            agentClient,
            resolver,
            "https://example.org",
            true,
            RetryPolicy.noRetry()
        );

        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("ok", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(AgentResponse.failure("boom", 1));

        Plan plan = planWithSingleStep();
        RecordingCallback callback = new RecordingCallback();

        List<StepExecutionResult> results = service.executePlan(plan, callback);

        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
        assertTrue(callback.events.contains("planStarted"));
        assertTrue(callback.events.contains("stepStarted:0"));
        assertTrue(callback.events.contains("stepCompleted:0:false"));
        assertTrue(callback.events.contains("planCompleted:false"));
    }

    @Test
    void noOpCallbackShouldNotThrow() {
        StepExecutionCallback callback = StepExecutionCallback.noOp();
        Plan plan = new Plan("p", "wf", "new", "s1", null, null, List.of());
        PlanStep step = new PlanStep("s1", "p", "wf", "wait", "ent-page", "result", 0, "Wait", List.of());
        StepExecutionResult result = StepExecutionResult.success("wait", "result", "ok", 1, null);

        assertDoesNotThrow(() -> callback.onPlanStarted(plan));
        assertDoesNotThrow(() -> callback.onStepStarted(step, 0, 1));
        assertDoesNotThrow(() -> callback.onStepCompleted(step, result, 0));
        assertDoesNotThrow(() -> callback.onPlanCompleted(plan, List.of(result), true));
    }

    private Plan planWithSingleStep() {
        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "wait", "ent-page", "result", 0, "Wait", List.of(new PlanStepAction("a1", "10"))
        );
        return new Plan("p1", "wf-plan", "new", "s1", null, null, List.of(step));
    }

    private Plan planWithTwoSteps() {
        PlanStep s1 = new PlanStep(
            "s1", "p1", "wf", "wait", "ent-page", "result", 0, "Wait1", List.of(new PlanStepAction("a1", "10"))
        );
        PlanStep s2 = new PlanStep(
            "s2", "p1", "wf", "wait", "ent-page", "result", 1, "Wait2", List.of(new PlanStepAction("a1", "10"))
        );
        return new Plan("p1", "wf-plan", "new", "s1", null, null, List.of(s1, s2));
    }

    private static class RecordingCallback implements StepExecutionCallback {
        private final List<String> events = new ArrayList<>();

        @Override
        public void onStepStarted(PlanStep step, int stepIndex, int totalSteps) {
            events.add("stepStarted:" + stepIndex);
        }

        @Override
        public void onStepCompleted(PlanStep step, StepExecutionResult result, int stepIndex) {
            events.add("stepCompleted:" + stepIndex + ":" + result.success());
        }

        @Override
        public void onPlanStarted(Plan plan) {
            events.add("planStarted");
        }

        @Override
        public void onPlanCompleted(Plan plan, List<StepExecutionResult> results, boolean success) {
            events.add("planCompleted:" + success);
        }
    }
}
