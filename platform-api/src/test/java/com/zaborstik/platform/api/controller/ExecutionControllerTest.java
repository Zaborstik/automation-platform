package com.zaborstik.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.exception.GlobalExceptionHandler;
import com.zaborstik.platform.api.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExecutionController.class)
@Import(GlobalExceptionHandler.class)
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionService executionService;

    private EntityDTO testPlan;

    @BeforeEach
    void setUp() {
        testPlan = new EntityDTO(EntityDTO.TABLE_PLANS, "test-plan-id",
                Map.of("entityTypeId", "Building", "entityId", "93939", "actionId", "order_egrn_extract",
                        "status", "CREATED", "steps", List.of()));
    }

    @Test
    void shouldCreatePlanSuccessfully() throws Exception {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of()));

        when(executionService.createPlan(any(EntityDTO.class))).thenReturn(testPlan);

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableName").value(EntityDTO.TABLE_PLANS))
                .andExpect(jsonPath("$.id").value("test-plan-id"))
                .andExpect(jsonPath("$.data.entityTypeId").value("Building"))
                .andExpect(jsonPath("$.data.entityId").value("93939"))
                .andExpect(jsonPath("$.data.actionId").value("order_egrn_extract"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void shouldReturnBadRequestWhenTableNameNotExecutionRequest() throws Exception {
        EntityDTO request = new EntityDTO("other_table", null, Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract"));

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleServiceException() throws Exception {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract"));

        when(executionService.createPlan(any(EntityDTO.class)))
                .thenThrow(new IllegalArgumentException("EntityType not found: Building"));

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").value("EntityType not found: Building"));
    }

    @Test
    void shouldReturnPlanWhenFound() throws Exception {
        when(executionService.getPlan("test-plan-id")).thenReturn(java.util.Optional.of(testPlan));

        mockMvc.perform(get("/api/execution/plan/test-plan-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableName").value(EntityDTO.TABLE_PLANS))
                .andExpect(jsonPath("$.id").value("test-plan-id"))
                .andExpect(jsonPath("$.data.entityTypeId").value("Building"))
                .andExpect(jsonPath("$.data.entityId").value("93939"))
                .andExpect(jsonPath("$.data.actionId").value("order_egrn_extract"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void shouldReturnNotFoundWhenPlanDoesNotExist() throws Exception {
        when(executionService.getPlan("non-existent-id")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/execution/plan/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Plan with id 'non-existent-id' not found"));
    }

    @Test
    void shouldAcceptRequestWithParameters() throws Exception {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of("param1", "value1")));

        when(executionService.createPlan(any(EntityDTO.class))).thenReturn(testPlan);

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
