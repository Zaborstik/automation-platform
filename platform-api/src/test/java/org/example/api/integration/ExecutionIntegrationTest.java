package org.example.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.dto.ExecutionRequestDTO;
import org.example.api.dto.PlanDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.example.api.PlatformApiApplication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PlatformApiApplication.class)
@AutoConfigureMockMvc
class ExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreatePlanForBuildingEntity() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When
        MvcResult result = mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.entityType").value("Building"))
            .andExpect(jsonPath("$.entityId").value("93939"))
            .andExpect(jsonPath("$.action").value("order_egrn_extract"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.steps").isArray())
            .andExpect(jsonPath("$.steps.length()").value(5))
            .andReturn();

        // Then - проверяем структуру ответа
        String responseContent = result.getResponse().getContentAsString();
        PlanDTO plan = objectMapper.readValue(responseContent, PlanDTO.class);
        
        assertNotNull(plan);
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertEquals(5, plan.getSteps().size());
        
        // Проверяем типы шагов
        assertEquals("open_page", plan.getSteps().get(0).getType());
        assertEquals("explain", plan.getSteps().get(1).getType());
        assertEquals("hover", plan.getSteps().get(2).getType());
        assertEquals("click", plan.getSteps().get(3).getType());
        assertEquals("wait", plan.getSteps().get(4).getType());
    }

    @Test
    void shouldCreatePlanForContractEntity() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Contract",
            "contract-123",
            "close_contract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.entityType").value("Contract"))
            .andExpect(jsonPath("$.entityId").value("contract-123"))
            .andExpect(jsonPath("$.action").value("close_contract"))
            .andExpect(jsonPath("$.steps.length()").value(5));
    }

    @Test
    void shouldReturnBadRequestForNonExistentEntityType() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "NonExistent",
            "123",
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid Request"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestForNonApplicableAction() throws Exception {
        // Given - order_egrn_extract применимо только к Building, а не к Contract
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Contract",
            "123",
            "order_egrn_extract",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    void shouldReturnBadRequestForNonExistentAction() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "123",
            "non_existent_action",
            Map.of()
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    void shouldHandleRequestWithParameters() throws Exception {
        // Given
        ExecutionRequestDTO request = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of("param1", "value1", "param2", 123, "param3", true)
        );

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnValidationErrorForEmptyEntityType() throws Exception {
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldReturnValidationErrorForMissingFields() throws Exception {
        // Given - создаем JSON без обязательных полей
        String invalidJson = "{\"entity\":\"Building\"}";

        // When & Then
        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }
}

