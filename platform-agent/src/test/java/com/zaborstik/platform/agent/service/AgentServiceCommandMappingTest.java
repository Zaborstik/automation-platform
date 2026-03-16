package com.zaborstik.platform.agent.service;

import com.zaborstik.platform.agent.client.AgentClient;
import com.zaborstik.platform.agent.dto.AgentCommand;
import com.zaborstik.platform.agent.dto.AgentResponse;
import com.zaborstik.platform.agent.dto.RetryPolicy;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import com.zaborstik.platform.core.resolver.Resolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceCommandMappingTest {

    @Mock
    private AgentClient agentClient;

    @Mock
    private Resolver resolver;

    private AgentService service;

    @BeforeEach
    void setUp() {
        service = new AgentService(agentClient, resolver, "https://example.org", true, RetryPolicy.noRetry());
    }

    @Test
    void shouldMapSelectOptionStepToSelectOptionCommand() throws Exception {
        when(agentClient.initialize(any(), anyBoolean())).thenReturn(AgentResponse.success("ok", Map.of(), 0));
        when(agentClient.execute(any())).thenReturn(AgentResponse.success("done", Map.of(), 1));

        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "select_option", "ent-input", "#country", 0, "Select country",
            List.of(new PlanStepAction("a1", "DE"))
        );
        service.executePlan(plan(step));

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentClient).execute(captor.capture());
        AgentCommand command = captor.getValue();
        assertEquals(AgentCommand.CommandType.SELECT_OPTION, command.getType());
        assertEquals("#country", command.getTarget());
        assertEquals("DE", command.getParameters().get("value"));
    }

    @Test
    void shouldMapReadTextStepToReadTextCommand() throws Exception {
        when(agentClient.initialize(any(), anyBoolean())).thenReturn(AgentResponse.success("ok", Map.of(), 0));
        when(agentClient.execute(any())).thenReturn(AgentResponse.success("done", Map.of(), 1));

        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "read_text", "ent-input", "#result", 0, "Read result", List.of()
        );
        service.executePlan(plan(step));

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentClient).execute(captor.capture());
        AgentCommand command = captor.getValue();
        assertEquals(AgentCommand.CommandType.READ_TEXT, command.getType());
        assertEquals("#result", command.getTarget());
    }

    @Test
    void shouldMapTakeScreenshotAndExtractScreenshotPath() throws Exception {
        when(agentClient.initialize(any(), anyBoolean())).thenReturn(AgentResponse.success("ok", Map.of(), 0));
        when(agentClient.execute(any()))
            .thenReturn(AgentResponse.success("done", Map.of("screenshot", "/tmp/screen.png"), 1));

        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "take_screenshot", "ent-page", "fullpage", 0, "Take screen", List.of()
        );

        List<StepExecutionResult> results = service.executePlan(plan(step));

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentClient).execute(captor.capture());
        AgentCommand command = captor.getValue();
        assertEquals(AgentCommand.CommandType.SCREENSHOT, command.getType());
        assertEquals("fullpage", command.getTarget());
        assertEquals("/tmp/screen.png", results.get(0).getScreenshotPath());
    }

    @Test
    void shouldUseEmptyStringWhenSelectOptionMetaValueMissing() throws Exception {
        when(agentClient.initialize(any(), anyBoolean())).thenReturn(AgentResponse.success("ok", Map.of(), 0));
        when(agentClient.execute(any())).thenReturn(AgentResponse.success("done", Map.of(), 1));

        PlanStep step = new PlanStep(
            "s1", "p1", "wf", "select_option", "ent-input", "#country", 0, "Select country",
            List.of(new PlanStepAction("a1", null))
        );
        service.executePlan(plan(step));

        ArgumentCaptor<AgentCommand> captor = ArgumentCaptor.forClass(AgentCommand.class);
        verify(agentClient).execute(captor.capture());
        AgentCommand command = captor.getValue();
        assertEquals("", command.getParameters().get("value"));
    }

    private Plan plan(PlanStep step) {
        return new Plan("p1", "wf-plan", "new", step.id(), null, null, List.of(step));
    }
}
