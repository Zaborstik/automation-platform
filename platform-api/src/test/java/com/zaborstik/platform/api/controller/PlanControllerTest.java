package com.zaborstik.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.ExecutePlanResponse;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.dto.TransitionPlanRequest;
import com.zaborstik.platform.api.exception.GlobalExceptionHandler;
import com.zaborstik.platform.api.service.PlanExecutionService;
import com.zaborstik.platform.api.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import(GlobalExceptionHandler.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlanService planService;

    @MockBean
    private PlanExecutionService planExecutionService;

    @Test
    void shouldCreatePlanSuccessfully() throws Exception {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setWorkflowId("wf-plan");
        request.setWorkflowStepInternalName("new");
        request.setTarget("Получить выписку ЕГРН");
        request.setExplanation("План для здания 93939");
        request.setSteps(List.of(stepRequest("et-building", "e-93939", 0, "Открыть карточку",
                List.of(actionRequest("act-open", "https://example.com/buildings/93939")))));

        PlanResponse created = new PlanResponse();
        created.setId("plan-1");
        created.setWorkflowId("wf-plan");
        created.setWorkflowStepInternalName("new");
        created.setTarget(request.getTarget());
        created.setExplanation(request.getExplanation());

        when(planService.createPlan(any(CreatePlanRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/plans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.workflowId").value("wf-plan"))
                .andExpect(jsonPath("$.target").value("Получить выписку ЕГРН"));
    }

    @Test
    void shouldListPlans() throws Exception {
        PlanResponse plan = new PlanResponse();
        plan.setId("plan-1");
        plan.setWorkflowStepInternalName("new");
        Page<PlanResponse> page = new PageImpl<>(List.of(plan), PageRequest.of(0, 20), 1);

        when(planService.listPlans(null, PageRequest.of(0, 20))).thenReturn(page);

        mockMvc.perform(get("/api/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("plan-1"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldListPlansWithStatusFilter() throws Exception {
        PlanResponse plan = new PlanResponse();
        plan.setId("plan-2");
        plan.setWorkflowStepInternalName("new");
        Page<PlanResponse> page = new PageImpl<>(List.of(plan), PageRequest.of(0, 20), 1);

        when(planService.listPlans("new", PageRequest.of(0, 20))).thenReturn(page);

        mockMvc.perform(get("/api/plans").param("status", "new"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("plan-2"));
    }

    @Test
    void shouldListPlansWithPagination() throws Exception {
        PlanResponse plan = new PlanResponse();
        plan.setId("plan-3");
        Page<PlanResponse> page = new PageImpl<>(List.of(plan), PageRequest.of(0, 5), 1);

        when(planService.listPlans(null, PageRequest.of(0, 5))).thenReturn(page);

        mockMvc.perform(get("/api/plans").param("page", "0").param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldReturnPlanWhenFound() throws Exception {
        PlanResponse plan = new PlanResponse();
        plan.setId("plan-1");
        plan.setWorkflowId("wf-plan");
        plan.setTarget("Test target");

        when(planService.getPlan("plan-1")).thenReturn(java.util.Optional.of(plan));

        mockMvc.perform(get("/api/plans/plan-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("plan-1"))
                .andExpect(jsonPath("$.target").value("Test target"));
    }

    @Test
    void shouldReturnNotFoundWhenPlanDoesNotExist() throws Exception {
        when(planService.getPlan("non-existent-id")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/plans/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Plan with id 'non-existent-id' not found"));
    }

    @Test
    void shouldExecutePlanSuccessfully() throws Exception {
        ExecutePlanResponse response = new ExecutePlanResponse();
        response.setPlanId("plan-1");
        response.setPlanResultId("result-1");
        response.setSuccess(true);
        response.setTotalSteps(2);
        response.setFailedSteps(0);
        when(planExecutionService.executePlan("plan-1")).thenReturn(java.util.Optional.of(response));

        mockMvc.perform(post("/api/plans/plan-1/execute"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.planId").value("plan-1"))
            .andExpect(jsonPath("$.planResultId").value("result-1"))
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldTransitionPlanSuccessfully() throws Exception {
        TransitionPlanRequest request = new TransitionPlanRequest();
        request.setTargetStep("in_progress");

        PlanResponse response = new PlanResponse();
        response.setId("plan-1");
        response.setWorkflowStepInternalName("in_progress");
        when(planService.transitionPlan("plan-1", "in_progress")).thenReturn(response);

        mockMvc.perform(patch("/api/plans/plan-1/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowStepInternalName").value("in_progress"));
    }

    @Test
    void shouldReturnNotFoundOnTransitionForMissingPlan() throws Exception {
        TransitionPlanRequest request = new TransitionPlanRequest();
        request.setTargetStep("in_progress");
        when(planService.transitionPlan("missing", "in_progress"))
            .thenThrow(new NoSuchElementException("Plan not found: missing"));

        mockMvc.perform(patch("/api/plans/missing/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictOnInvalidTransition() throws Exception {
        TransitionPlanRequest request = new TransitionPlanRequest();
        request.setTargetStep("completed");
        when(planService.transitionPlan("plan-1", "completed"))
            .thenThrow(new IllegalStateException("Transition is not allowed"));

        mockMvc.perform(patch("/api/plans/plan-1/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    private static CreatePlanRequest.PlanStepRequest stepRequest(String entityTypeId, String entityId, int sortOrder,
                                                                 String displayName,
                                                                 List<CreatePlanRequest.PlanStepActionRequest> actions) {
        CreatePlanRequest.PlanStepRequest s = new CreatePlanRequest.PlanStepRequest();
        s.setWorkflowId("wf-plan");
        s.setWorkflowStepInternalName("new");
        s.setEntityTypeId(entityTypeId);
        s.setEntityId(entityId);
        s.setSortOrder(sortOrder);
        s.setDisplayName(displayName);
        s.setActions(actions);
        return s;
    }

    private static CreatePlanRequest.PlanStepActionRequest actionRequest(String actionId, String metaValue) {
        CreatePlanRequest.PlanStepActionRequest a = new CreatePlanRequest.PlanStepActionRequest();
        a.setActionId(actionId);
        a.setMetaValue(metaValue);
        return a;
    }
}
