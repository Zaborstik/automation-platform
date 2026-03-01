package com.zaborstik.platform.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DTOsSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void shouldSerializeAndDeserializeEntityDTO() throws Exception {
        EntityDTO original = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of("param1", "value1")));

        String json = objectMapper.writeValueAsString(original);
        EntityDTO deserialized = objectMapper.readValue(json, EntityDTO.class);

        assertNotNull(json);
        assertEquals(EntityDTO.TABLE_EXECUTION_REQUEST, deserialized.getTableName());
        assertEquals("Building", deserialized.get("entity"));
        assertEquals("93939", deserialized.get("entityId"));
        assertEquals("order_egrn_extract", deserialized.get("action"));
        assertEquals("value1", ((Map<?, ?>) deserialized.get("parameters")).get("param1"));
    }

    @Test
    void shouldSerializeAndDeserializeEntityDTOWithPlansTableName() throws Exception {
        EntityDTO original = new EntityDTO(EntityDTO.TABLE_PLANS, "plan-1",
                Map.of("entityTypeId", "Building", "entityId", "93939", "actionId", "order_egrn_extract", "status", "CREATED", "steps", List.of()));

        String json = objectMapper.writeValueAsString(original);
        EntityDTO deserialized = objectMapper.readValue(json, EntityDTO.class);

        assertNotNull(json);
        assertEquals(EntityDTO.TABLE_PLANS, deserialized.getTableName());
        assertEquals("plan-1", deserialized.getId());
        assertEquals("Building", deserialized.get("entityTypeId"));
        assertEquals("CREATED", deserialized.get("status"));
    }

    @Test
    void shouldSerializeAndDeserializeAttachmentDTO() throws Exception {
        AttachmentDTO original = new AttachmentDTO("att-1", Map.of(
                "parentTable", "plans",
                "parentId", "plan-123",
                "filename", "doc.pdf",
                "contentType", "application/pdf"
        ));

        String json = objectMapper.writeValueAsString(original);
        AttachmentDTO deserialized = objectMapper.readValue(json, AttachmentDTO.class);

        assertNotNull(json);
        assertEquals(EntityDTO.TABLE_ATTACHMENTS, deserialized.getTableName());
        assertEquals("att-1", deserialized.getId());
        assertEquals("plans", deserialized.getParentTable());
        assertEquals("plan-123", deserialized.getParentId());
        assertEquals("doc.pdf", deserialized.getFilename());
        assertEquals("application/pdf", deserialized.getContentType());
    }

    @Test
    void shouldSerializeAndDeserializeErrorResponseDTO() throws Exception {
        ErrorResponseDTO original = new ErrorResponseDTO(
                400, "Bad Request", "Validation failed", "/api/execution/plan"
        );

        String json = objectMapper.writeValueAsString(original);
        ErrorResponseDTO deserialized = objectMapper.readValue(json, ErrorResponseDTO.class);

        assertNotNull(json);
        assertEquals(400, deserialized.getStatus());
        assertEquals("Bad Request", deserialized.getError());
        assertEquals("Validation failed", deserialized.getMessage());
        assertEquals("/api/execution/plan", deserialized.getPath());
        assertNotNull(deserialized.getTimestamp());
    }

    @Test
    void shouldDistinguishByTableName() throws Exception {
        EntityDTO executionRequest = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null, Map.of("entity", "Building", "entityId", "1", "action", "a"));
        EntityDTO plan = new EntityDTO(EntityDTO.TABLE_PLANS, "p1", Map.of("entityTypeId", "Building", "status", "CREATED"));

        String jsonReq = objectMapper.writeValueAsString(executionRequest);
        String jsonPlan = objectMapper.writeValueAsString(plan);

        assertTrue(jsonReq.contains("\"tableName\":\"execution_request\""));
        assertTrue(jsonPlan.contains("\"tableName\":\"plans\""));
        assertEquals(EntityDTO.TABLE_EXECUTION_REQUEST, objectMapper.readValue(jsonReq, EntityDTO.class).getTableName());
        assertEquals(EntityDTO.TABLE_PLANS, objectMapper.readValue(jsonPlan, EntityDTO.class).getTableName());
    }

    @Test
    void shouldSerializeAndDeserializeCreatePlanRequestAndPlanResponse() throws Exception {
        CreatePlanRequest req = new CreatePlanRequest();
        req.setWorkflowId("wf-plan");
        req.setWorkflowStepInternalName("new");
        req.setTarget("Цель");
        req.setExplanation("Пояснение");
        CreatePlanRequest.PlanStepRequest step = new CreatePlanRequest.PlanStepRequest();
        step.setWorkflowId("wf-plan-step");
        step.setWorkflowStepInternalName("new");
        step.setEntityTypeId("ent-page");
        step.setEntityId("e-1");
        step.setSortOrder(0);
        step.setDisplayName("Шаг 1");
        CreatePlanRequest.PlanStepActionRequest act = new CreatePlanRequest.PlanStepActionRequest();
        act.setActionId("act-open-page");
        act.setMetaValue("https://example.com");
        step.setActions(List.of(act));
        req.setSteps(List.of(step));

        String jsonReq = objectMapper.writeValueAsString(req);
        CreatePlanRequest deserializedReq = objectMapper.readValue(jsonReq, CreatePlanRequest.class);
        assertNotNull(jsonReq);
        assertEquals("wf-plan", deserializedReq.getWorkflowId());
        assertEquals("Цель", deserializedReq.getTarget());
        assertEquals(1, deserializedReq.getSteps().size());
        assertEquals("act-open-page", deserializedReq.getSteps().get(0).getActions().get(0).getActionId());

        PlanResponse resp = new PlanResponse();
        resp.setId("plan-1");
        resp.setWorkflowId("wf-plan");
        resp.setTarget("Target");
        String jsonResp = objectMapper.writeValueAsString(resp);
        PlanResponse deserializedResp = objectMapper.readValue(jsonResp, PlanResponse.class);
        assertEquals("plan-1", deserializedResp.getId());
        assertEquals("Target", deserializedResp.getTarget());
    }
}
