package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.client.AgentException;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentClient agentClient;

    @Test
    void executeStepMapsOpenPageToCommandAndReturnsSuccess() throws AgentException {
        AgentService service = new AgentService(agentClient, RetryPolicy.noRetry());
        PlanStep step = step("open https://example.com", "https://example.com",
            new PlanStepAction("act-open-page", "https://example.com"));

        when(agentClient.execute(any(AgentCommand.class)))
            .thenReturn(AgentResponse.success("ok", Map.of(), 10L));

        StepExecutionResult result = service.executeStep(step, "open_page", Map.of(), 0);

        assertTrue(result.success());
        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentClient).execute(captor.capture());
        assertEquals(AgentCommand.CommandType.OPEN_PAGE, captor.getValue().type());
        assertEquals("https://example.com", captor.getValue().target());
    }

    @Test
    void executeStepReturnsFailureOnUnknownOperation() {
        AgentService service = new AgentService(agentClient, RetryPolicy.noRetry());
        PlanStep step = step("noop", null, new PlanStepAction("act-noop", null));

        StepExecutionResult result = service.executeStep(step, "definitely-unknown", Map.of(), 0);

        assertFalse(result.success());
    }

    private static PlanStep step(String name, String entityId, PlanStepAction action) {
        return new PlanStep(
            "step-id",
            "plan-id",
            "wf-plan-step",
            "new",
            "ent-page",
            entityId,
            0,
            name,
            List.of(action)
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
