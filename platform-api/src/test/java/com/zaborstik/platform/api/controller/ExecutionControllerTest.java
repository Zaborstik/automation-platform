package com.zaborstik.platform.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
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

    private PlanDTO testPlan;

    @BeforeEach
    void setUp() {
        testPlan = new PlanDTO(
            "test-plan-id",
            "Building",
            "93939",
            "order_egrn_extract",
            List.of(),
            "CREATED"
        );
    }

    @Test
    void shouldCreatePlanSuccessfully() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionService.createPlan(any(ExecutionRequestDTO.class))).thenReturn(testPlan);

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("test-plan-id"))
            .andExpect(jsonPath("$.entityType").value("Building"))
            .andExpect(jsonPath("$.entityId").value("93939"))
            .andExpect(jsonPath("$.action").value("order_egrn_extract"))
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void shouldReturnBadRequestWhenEntityTypeIsMissing() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            null,
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldReturnBadRequestWhenEntityIdIsMissing() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            null,
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldReturnBadRequestWhenActionIsMissing() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            null,
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldReturnBadRequestWhenEntityTypeIsEmpty() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleServiceException() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionService.createPlan(any(ExecutionRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("EntityType not found: Building"));

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid Request"))
            .andExpect(jsonPath("$.message").value("EntityType not found: Building"));
    }

    @Test
    void shouldReturnNotImplementedForGetPlan() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/execution/plan/test-id"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.error").value("Not Implemented"))
            .andExpect(jsonPath("$.message").value("Plan storage is not implemented yet. Plans are created on-demand."));
    }

    @Test
    void shouldAcceptRequestWithParameters() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of("param1", "value1", "param2", 123)
        );

        when(executionService.createPlan(any(ExecutionRequestDTO.class))).thenReturn(testPlan);

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}

