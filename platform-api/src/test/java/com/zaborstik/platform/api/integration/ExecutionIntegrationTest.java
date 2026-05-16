package com.zaborstik.platform.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.PlatformApiApplication;
import com.zaborstik.platform.api.client.KnowledgeClient;
import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.FinishPlanRunRequest;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.dto.PlanRunResponse;
import com.zaborstik.platform.api.dto.StepExecutionReportRequest;
import com.zaborstik.platform.api.repository.AttachmentRepository;
import com.zaborstik.platform.api.repository.PlanResultRepository;
import com.zaborstik.platform.api.repository.PlanStepLogRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for the server-side run lifecycle. The plan is created via
 * the REST endpoint, then the local executor flow is simulated with manual
 * HTTP calls: start the run, report a successful step, report a failed step,
 * then finish the run. We verify that lifecycle transitions, plan_result and
 * plan_step_log records all land in the DB.
 */
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
    private PlanStepLogRepository planStepLogRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @MockBean
    private KnowledgeClient knowledgeClient;

    @Test
    void shouldCreateAndRetrievePlan() throws Exception {
        CreatePlanRequest request = buildBasicCreatePlanRequest("Открыть страницу и ввести текст");

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

        mockMvc.perform(get("/api/plans/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.steps.length()").value(2));
    }

    @Test
    void shouldReturnNotFoundForNonExistentPlan() throws Exception {
        mockMvc.perform(get("/api/plans/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldRunPlanLifecycleFromLocalExecutorAndPersistFailureLog() throws Exception {
        CreatePlanRequest request = buildBasicCreatePlanRequest("Клик и ввод");

        MvcResult createResult = mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        PlanResponse created = objectMapper.readValue(createResult.getResponse().getContentAsString(), PlanResponse.class);

        long attachmentsBefore = attachmentRepository.count();

        MvcResult startResult = mockMvc.perform(post("/api/plans/" + created.getId() + "/runs"))
                .andExpect(status().isOk())
                .andReturn();
        PlanRunResponse run = objectMapper.readValue(startResult.getResponse().getContentAsString(), PlanRunResponse.class);
        assertNotNull(run.getPlanResultId());

        StepExecutionReportRequest successReport = new StepExecutionReportRequest();
        successReport.setSuccess(true);
        successReport.setActionId("act-click");
        successReport.setMessage("Clicked");
        successReport.setExecutedAt(Instant.parse("2026-03-15T11:00:01Z"));
        successReport.setExecutionTimeMs(120L);
        mockMvc.perform(post("/api/plans/" + created.getId() + "/steps/"
                + created.getSteps().get(0).getId() + "/result")
                .param("planResultId", run.getPlanResultId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(successReport)))
                .andExpect(status().isNoContent());

        StepExecutionReportRequest failureReport = new StepExecutionReportRequest();
        failureReport.setSuccess(false);
        failureReport.setActionId("act-input-text");
        failureReport.setMessage("Input not possible");
        failureReport.setError("Element not visible");
        failureReport.setExecutedAt(Instant.parse("2026-03-15T11:00:02Z"));
        failureReport.setExecutionTimeMs(180L);
        failureReport.setScreenshotPath("/tmp/step-error.png");
        mockMvc.perform(post("/api/plans/" + created.getId() + "/steps/"
                + created.getSteps().get(1).getId() + "/result")
                .param("planResultId", run.getPlanResultId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failureReport)))
                .andExpect(status().isNoContent());

        FinishPlanRunRequest finish = new FinishPlanRunRequest();
        finish.setPlanResultId(run.getPlanResultId());
        finish.setSuccess(false);
        finish.setTotalSteps(2);
        finish.setFailedSteps(1);
        finish.setStartedTime(Instant.parse("2026-03-15T11:00:00Z"));
        finish.setFinishedTime(Instant.parse("2026-03-15T11:00:03Z"));
        mockMvc.perform(post("/api/plans/" + created.getId() + "/runs/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(finish)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.failedSteps").value(1));

        assertTrue(planResultRepository.findByPlan_Id(created.getId()).isPresent());
        assertEquals(1, planStepLogRepository.findByPlan_Id(created.getId()).size());
        assertEquals(attachmentsBefore + 1, attachmentRepository.count());
    }

    private CreatePlanRequest buildBasicCreatePlanRequest(String target) {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setWorkflowId("wf-plan");
        request.setWorkflowStepInternalName("new");
        request.setTarget(target);
        request.setExplanation("План для проверки API " + UUID.randomUUID());

        CreatePlanRequest.PlanStepRequest step1 = new CreatePlanRequest.PlanStepRequest();
        step1.setWorkflowId("wf-plan-step");
        step1.setWorkflowStepInternalName("new");
        step1.setEntityTypeId("ent-button");
        step1.setEntityId("#submit");
        step1.setSortOrder(0);
        step1.setDisplayName("Клик");
        CreatePlanRequest.PlanStepActionRequest a1 = new CreatePlanRequest.PlanStepActionRequest();
        a1.setActionId("act-click");
        step1.setActions(List.of(a1));

        CreatePlanRequest.PlanStepRequest step2 = new CreatePlanRequest.PlanStepRequest();
        step2.setWorkflowId("wf-plan-step");
        step2.setWorkflowStepInternalName("new");
        step2.setEntityTypeId("ent-input");
        step2.setEntityId("#search");
        step2.setSortOrder(1);
        step2.setDisplayName("Ввод текста");
        CreatePlanRequest.PlanStepActionRequest a2 = new CreatePlanRequest.PlanStepActionRequest();
        a2.setActionId("act-input-text");
        a2.setMetaValue("query");
        step2.setActions(List.of(a2));

        request.setSteps(List.of(step1, step2));
        return request;
    }
}
