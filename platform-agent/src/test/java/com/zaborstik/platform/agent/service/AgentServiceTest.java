package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentClient agentClient;

    @Mock
    private Resolver resolver;

    private AgentService agentService;
    private Plan oneStepPlan;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(
            agentClient,
            resolver,
            "https://example.org",
            true,
            new RetryPolicy(2, 0, List.of("timeout", "not found", "not visible"))
        );

        PlanStep step = new PlanStep(
            "step-1",
            "plan-1",
            "wf-plan-step",
            "new",
            "ent-page",
            "result",
            0,
            "Wait result",
            List.of(new PlanStepAction("act-1", "50"))
        );

        when(resolver.findAction("act-1"))
            .thenReturn(Optional.of(Action.of("act-1", "Wait", "wait", "Wait", "act-type-validation")));

        oneStepPlan = new Plan(
            "plan-1",
            "wf-plan",
            "new",
            "step-1",
            "target",
            "explanation",
            List.of(step)
        );
    }

    @Test
    void shouldSucceedFromFirstAttempt() throws Exception {
        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("initialized", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(AgentResponse.success("ok", java.util.Map.of(), 10));

        List<StepExecutionResult> results = agentService.executePlan(oneStepPlan);

        assertEquals(1, results.size());
        assertTrue(results.get(0).success());
        assertEquals(0, results.get(0).retryCount());
        verify(agentClient).execute(any());
    }

    @Test
    void shouldRetryForRetryableFailureUntilLimit() throws Exception {
        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("initialized", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(AgentResponse.failure("timeout while waiting element", 10));

        List<StepExecutionResult> results = agentService.executePlan(oneStepPlan);

        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
        assertEquals(2, results.get(0).retryCount());
        verify(agentClient, org.mockito.Mockito.times(3)).execute(any());
    }

    @Test
    void shouldNotRetryForNonRetryableFailure() throws Exception {
        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("initialized", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(AgentResponse.failure("something unexpected", 10));

        List<StepExecutionResult> results = agentService.executePlan(oneStepPlan);

        assertEquals(1, results.size());
        assertFalse(results.get(0).success());
        assertEquals(0, results.get(0).retryCount());
        verify(agentClient, org.mockito.Mockito.times(1)).execute(any());
    }

    @Test
    void shouldSucceedOnSecondAttempt() throws Exception {
        when(agentClient.initialize(any(), anyBoolean()))
            .thenReturn(AgentResponse.success("initialized", java.util.Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(
                AgentResponse.failure("timeout", 10),
                AgentResponse.success("ok", java.util.Map.of(), 10)
            );

        List<StepExecutionResult> results = agentService.executePlan(oneStepPlan);

        assertEquals(1, results.size());
        assertTrue(results.get(0).success());
        assertEquals(1, results.get(0).retryCount());
        verify(agentClient, org.mockito.Mockito.times(2)).execute(any());
    }
}
