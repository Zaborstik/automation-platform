package com.zaborstik.platform.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.ExecutePlanResponse;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.PlatformApiApplication;
import com.zaborstik.platform.api.repository.AttachmentRepository;
import com.zaborstik.platform.api.repository.PlanResultRepository;
import com.zaborstik.platform.api.repository.PlanStepLogEntryRepository;
import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.executor.ExecutionLogEntry;
import com.zaborstik.platform.executor.PlanExecutionResult;
import com.zaborstik.platform.executor.PlanExecutor;
import com.zaborstik.platform.core.plan.Plan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PlatformApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlanResultRepository planResultRepository;

    @Autowired
    private PlanStepLogEntryRepository planStepLogEntryRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @MockBean
    private PlanExecutor planExecutor;

    @MockBean
    private AgentService agentService;

    @Test
    void shouldCreateAndRetrievePlan() throws Exception {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setWorkflowId("wf-plan");
        request.setWorkflowStepInternalName("new");
        request.setTarget("Открыть страницу и ввести текст");
        request.setExplanation("План для проверки API");
        CreatePlanRequest.PlanStepRequest step1 = new CreatePlanRequest.PlanStepRequest();
        step1.setWorkflowId("wf-plan-step");
        step1.setWorkflowStepInternalName("new");
        step1.setEntityTypeId("ent-page");
        step1.setEntityId("page-1");
        step1.setSortOrder(0);
        step1.setDisplayName("Открыть страницу");
        CreatePlanRequest.PlanStepActionRequest a1 = new CreatePlanRequest.PlanStepActionRequest();
        a1.setActionId("act-open-page");
        a1.setMetaValue("https://example.com");
        step1.setActions(List.of(a1));
        CreatePlanRequest.PlanStepRequest step2 = new CreatePlanRequest.PlanStepRequest();
        step2.setWorkflowId("wf-plan-step");
        step2.setWorkflowStepInternalName("new");
        step2.setEntityTypeId("ent-input");
        step2.setEntityId("input-1");
        step2.setSortOrder(1);
        step2.setDisplayName("Ввести текст");
        CreatePlanRequest.PlanStepActionRequest a2 = new CreatePlanRequest.PlanStepActionRequest();
        a2.setActionId("act-input-text");
        a2.setMetaValue("test value");
        step2.setActions(List.of(a2));
        request.setSteps(List.of(step1, step2));

        MvcResult createResult = mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.workflowId").value("wf-plan"))
                .andExpect(jsonPath("$.target").value("Открыть страницу и ввести текст"))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andReturn();

        PlanResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PlanResponse.class);
        assertNotNull(created.getId());
        assertEquals(2, created.getSteps().size());
        assertEquals("act-open-page", created.getSteps().get(0).getActions().get(0).getActionId());
        assertEquals("https://example.com", created.getSteps().get(0).getActions().get(0).getMetaValue());

        mockMvc.perform(get("/api/plans/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.target").value("Открыть страницу и ввести текст"))
                .andExpect(jsonPath("$.steps.length()").value(2));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPlan() throws Exception {
        mockMvc.perform(get("/api/plans/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Plan with id 'non-existent-id' not found"));
    }

    @Test
    void shouldCreatePlanWithEmptySteps() throws Exception {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setWorkflowId("wf-plan");
        request.setWorkflowStepInternalName("new");
        request.setTarget("Пустой план");
        request.setSteps(List.of());

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps.length()").value(0));
    }

    @Test
    void shouldExecutePlanAndPersistFailureLogWithAttachment() throws Exception {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setWorkflowId("wf-plan");
        request.setWorkflowStepInternalName("new");
        request.setTarget("Клик и ввод");
        request.setExplanation("План для проверки execution endpoint");

        CreatePlanRequest.PlanStepRequest step1 = new CreatePlanRequest.PlanStepRequest();
        step1.setWorkflowId("wf-plan-step");
        step1.setWorkflowStepInternalName("click");
        step1.setEntityTypeId("ent-button");
        step1.setEntityId("#submit");
        step1.setSortOrder(0);
        step1.setDisplayName("Клик по кнопке");
        CreatePlanRequest.PlanStepActionRequest a1 = new CreatePlanRequest.PlanStepActionRequest();
        a1.setActionId("act-click");
        step1.setActions(List.of(a1));

        CreatePlanRequest.PlanStepRequest step2 = new CreatePlanRequest.PlanStepRequest();
        step2.setWorkflowId("wf-plan-step");
        step2.setWorkflowStepInternalName("type");
        step2.setEntityTypeId("ent-input");
        step2.setEntityId("#search");
        step2.setSortOrder(1);
        step2.setDisplayName("Ввод текста");
        CreatePlanRequest.PlanStepActionRequest a2 = new CreatePlanRequest.PlanStepActionRequest();
        a2.setActionId("act-input-text");
        a2.setMetaValue("query");
        step2.setActions(List.of(a2));

        request.setSteps(List.of(step1, step2));

        MvcResult createResult = mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        PlanResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PlanResponse.class);

        long attachmentCountBefore = attachmentRepository.count();

        when(planExecutor.execute(any(Plan.class))).thenAnswer(invocation -> {
            Plan plan = invocation.getArgument(0);
            StepExecutionResult success = StepExecutionResult.success(
                plan.steps().get(0).id(),
                plan.steps().get(0).displayName(),
                "Clicked",
                120L,
                "/tmp/step-ok.png",
                Map.of("x", 120.0, "y", 240.0, "screenshot", "/tmp/step-ok.png")
            );
            StepExecutionResult failure = StepExecutionResult.failure(
                plan.steps().get(1).id(),
                plan.steps().get(1).displayName(),
                "Input not possible",
                180L,
                Map.of("x", 150.0, "y", 280.0, "screenshot", "/tmp/step-error.png")
            );
            return new PlanExecutionResult(
                plan.id(),
                false,
                Instant.parse("2026-03-15T11:00:00Z"),
                Instant.parse("2026-03-15T11:00:03Z"),
                List.of(
                    new ExecutionLogEntry(plan.id(), 0, plan.steps().get(0), success, Instant.now()),
                    new ExecutionLogEntry(plan.id(), 1, plan.steps().get(1), failure, Instant.now())
                )
            );
        });
        doNothing().when(agentService).close();

        MvcResult executeResult = mockMvc.perform(post("/api/plans/" + created.getId() + "/execute"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value(created.getId()))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.failedSteps").value(1))
            .andReturn();

        ExecutePlanResponse executeResponse = objectMapper.readValue(
            executeResult.getResponse().getContentAsString(),
            ExecutePlanResponse.class
        );
        assertNotNull(executeResponse.getPlanResultId());
        assertTrue(planResultRepository.findByPlan_Id(created.getId()).isPresent());
        assertEquals(1, planStepLogEntryRepository.findByPlan_Id(created.getId()).size());
        assertEquals(attachmentCountBefore + 1, attachmentRepository.count());
    }
}
