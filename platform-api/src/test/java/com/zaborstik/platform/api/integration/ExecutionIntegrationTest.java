package com.zaborstik.platform.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.PlatformApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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

    private EntityDTO executionRequest(String entity, String entityId, String action, Map<String, Object> parameters) {
        return new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", entity, "entityId", entityId, "action", action, "parameters", parameters != null ? parameters : Map.of()));
    }

    @Test
    void shouldCreatePlanForBuildingEntity() throws Exception {
        EntityDTO request = executionRequest("Building", "93939", "order_egrn_extract", Map.of());

        MvcResult result = mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableName").value(EntityDTO.TABLE_PLANS))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.data.entityTypeId").value("Building"))
                .andExpect(jsonPath("$.data.entityId").value("93939"))
                .andExpect(jsonPath("$.data.actionId").value("order_egrn_extract"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.steps").isArray())
                .andReturn();

        EntityDTO plan = objectMapper.readValue(result.getResponse().getContentAsString(), EntityDTO.class);
        assertEquals(EntityDTO.TABLE_PLANS, plan.getTableName());
        assertEquals("Building", plan.get("entityTypeId"));
        assertEquals("93939", plan.get("entityId"));
        assertEquals("order_egrn_extract", plan.get("actionId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) plan.get("steps");
        assertNotNull(steps);
        assertEquals(5, steps.size());
        assertEquals("open_page", steps.get(0).get("type"));
        assertEquals("explain", steps.get(1).get("type"));
        assertEquals("hover", steps.get(2).get("type"));
        assertEquals("click", steps.get(3).get("type"));
        assertEquals("wait", steps.get(4).get("type"));
    }

    @Test
    void shouldCreatePlanForContractEntity() throws Exception {
        EntityDTO request = executionRequest("Contract", "contract-123", "close_contract", Map.of());

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableName").value(EntityDTO.TABLE_PLANS))
                .andExpect(jsonPath("$.data.entityTypeId").value("Contract"))
                .andExpect(jsonPath("$.data.entityId").value("contract-123"))
                .andExpect(jsonPath("$.data.actionId").value("close_contract"))
                .andExpect(jsonPath("$.data.steps").isArray());
    }

    @Test
    void shouldReturnBadRequestForNonExistentEntityType() throws Exception {
        EntityDTO request = executionRequest("NonExistent", "123", "order_egrn_extract", Map.of());

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestForNonApplicableAction() throws Exception {
        EntityDTO request = executionRequest("Contract", "123", "order_egrn_extract", Map.of());

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    void shouldReturnBadRequestForNonExistentAction() throws Exception {
        EntityDTO request = executionRequest("Building", "123", "non_existent_action", Map.of());

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Request"));
    }

    @Test
    void shouldHandleRequestWithParameters() throws Exception {
        EntityDTO request = executionRequest("Building", "93939", "order_egrn_extract",
                Map.of("param1", "value1", "param2", 123, "param3", true));

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnBadRequestWhenDataMissingRequiredFields() throws Exception {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null, Map.of("entity", "Building"));

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenTableNameNotExecutionRequest() throws Exception {
        String invalidJson = "{\"tableName\":\"other\",\"id\":null,\"data\":{\"entity\":\"Building\",\"entityId\":\"93939\",\"action\":\"x\"}}";

        mockMvc.perform(post("/api/execution/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
