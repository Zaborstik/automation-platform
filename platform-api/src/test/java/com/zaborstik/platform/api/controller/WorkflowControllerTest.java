package com.zaborstik.platform.api.controller;

import com.zaborstik.platform.api.entity.WorkflowEntity;
import com.zaborstik.platform.api.entity.WorkflowStepEntity;
import com.zaborstik.platform.api.exception.GlobalExceptionHandler;
import com.zaborstik.platform.api.repository.WorkflowRepository;
import com.zaborstik.platform.api.repository.WorkflowStepRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@Import(GlobalExceptionHandler.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowRepository workflowRepository;

    @MockBean
    private WorkflowStepRepository workflowStepRepository;

    @Test
    void shouldGetAllWorkflows() throws Exception {
        when(workflowRepository.findAll()).thenReturn(List.of(workflow("wf-plan", "Plan workflow", "wfs-new")));

        mockMvc.perform(get("/api/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("wf-plan"))
            .andExpect(jsonPath("$[0].firstStepId").value("wfs-new"));
    }

    @Test
    void shouldGetWorkflowById() throws Exception {
        when(workflowRepository.findById("wf-plan")).thenReturn(Optional.of(workflow("wf-plan", "Plan workflow", "wfs-new")));

        mockMvc.perform(get("/api/workflows/wf-plan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("wf-plan"));
    }

    @Test
    void shouldReturn404WhenWorkflowNotFound() throws Exception {
        when(workflowRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/workflows/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workflow with id 'missing' not found"));
    }

    @Test
    void shouldGetAllWorkflowSteps() throws Exception {
        when(workflowStepRepository.findAll()).thenReturn(List.of(step("wfs-new", "new", "New", 10)));

        mockMvc.perform(get("/api/workflow-steps"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("wfs-new"))
            .andExpect(jsonPath("$[0].internalname").value("new"));
    }

    @Test
    void shouldGetWorkflowStepById() throws Exception {
        when(workflowStepRepository.findById("wfs-new")).thenReturn(Optional.of(step("wfs-new", "new", "New", 10)));

        mockMvc.perform(get("/api/workflow-steps/wfs-new"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("wfs-new"));
    }

    @Test
    void shouldReturn404WhenWorkflowStepMissing() throws Exception {
        when(workflowStepRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/workflow-steps/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Workflow step with id 'missing' not found"));
    }

    private static WorkflowEntity workflow(String id, String displayName, String firstStepId) {
        WorkflowStepEntity step = new WorkflowStepEntity();
        step.setId(firstStepId);

        WorkflowEntity workflow = new WorkflowEntity();
        workflow.setId(id);
        workflow.setDisplayname(displayName);
        workflow.setFirststep(step);
        return workflow;
    }

    private static WorkflowStepEntity step(String id, String internalName, String displayName, Integer sortOrder) {
        WorkflowStepEntity step = new WorkflowStepEntity();
        step.setId(id);
        step.setInternalname(internalName);
        step.setDisplayname(displayName);
        step.setSortorder(sortOrder);
        return step;
    }
}
