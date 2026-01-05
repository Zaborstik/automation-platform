package org.example.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    }

    @Test
    void shouldSerializeAndDeserializeExecutionRequestDTO() throws Exception {
        // Given
        ExecutionRequestDTO original = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of("param1", "value1", "param2", 123)
        );

        // When
        String json = objectMapper.writeValueAsString(original);
        ExecutionRequestDTO deserialized = objectMapper.readValue(json, ExecutionRequestDTO.class);

        // Then
        assertNotNull(json);
        assertEquals("Building", deserialized.getEntityType());
        assertEquals("93939", deserialized.getEntityId());
        assertEquals("order_egrn_extract", deserialized.getAction());
        assertNotNull(deserialized.getParameters());
        assertEquals("value1", deserialized.getParameters().get("param1"));
        assertEquals(123, deserialized.getParameters().get("param2"));
    }

    @Test
    void shouldSerializeAndDeserializeExecutionRequestDTOWithNullParameters() throws Exception {
        // Given
        ExecutionRequestDTO original = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            null
        );

        // When
        String json = objectMapper.writeValueAsString(original);
        ExecutionRequestDTO deserialized = objectMapper.readValue(json, ExecutionRequestDTO.class);

        // Then
        assertNotNull(json);
        assertEquals("Building", deserialized.getEntityType());
        assertEquals("93939", deserialized.getEntityId());
        assertEquals("order_egrn_extract", deserialized.getAction());
    }

    @Test
    void shouldSerializeAndDeserializePlanStepDTO() throws Exception {
        // Given
        PlanStepDTO original = new PlanStepDTO(
            "click",
            "action(order_egrn_extract)",
            "Выполняю действие",
            Map.of("highlight", true, "waitAfterClick", 2000)
        );

        // When
        String json = objectMapper.writeValueAsString(original);
        PlanStepDTO deserialized = objectMapper.readValue(json, PlanStepDTO.class);

        // Then
        assertNotNull(json);
        assertEquals("click", deserialized.getType());
        assertEquals("action(order_egrn_extract)", deserialized.getTarget());
        assertEquals("Выполняю действие", deserialized.getExplanation());
        assertNotNull(deserialized.getParameters());
        assertEquals(true, deserialized.getParameters().get("highlight"));
        assertEquals(2000, deserialized.getParameters().get("waitAfterClick"));
    }

    @Test
    void shouldSerializeAndDeserializePlanDTO() throws Exception {
        // Given
        PlanStepDTO step1 = new PlanStepDTO("open_page", "/buildings/93939", "Открываю карточку", Map.of());
        PlanStepDTO step2 = new PlanStepDTO("click", "action(order_egrn_extract)", "Кликаю", Map.of());
        
        PlanDTO original = new PlanDTO(
            "plan-id-123",
            "Building",
            "93939",
            "order_egrn_extract",
            List.of(step1, step2),
            "CREATED"
        );

        // When
        String json = objectMapper.writeValueAsString(original);
        PlanDTO deserialized = objectMapper.readValue(json, PlanDTO.class);

        // Then
        assertNotNull(json);
        assertEquals("plan-id-123", deserialized.getId());
        assertEquals("Building", deserialized.getEntityTypeId());
        assertEquals("93939", deserialized.getEntityId());
        assertEquals("order_egrn_extract", deserialized.getActionId());
        assertEquals("CREATED", deserialized.getStatus());
        assertNotNull(deserialized.getSteps());
        assertEquals(2, deserialized.getSteps().size());
        assertEquals("open_page", deserialized.getSteps().get(0).getType());
        assertEquals("click", deserialized.getSteps().get(1).getType());
    }

    @Test
    void shouldSerializeAndDeserializeErrorResponseDTO() throws Exception {
        // Given
        ErrorResponseDTO original = new ErrorResponseDTO(
            400,
            "Bad Request",
            "Validation failed",
            "/api/execution/plan"
        );

        // When
        String json = objectMapper.writeValueAsString(original);
        ErrorResponseDTO deserialized = objectMapper.readValue(json, ErrorResponseDTO.class);

        // Then
        assertNotNull(json);
        assertEquals(400, deserialized.getStatus());
        assertEquals("Bad Request", deserialized.getError());
        assertEquals("Validation failed", deserialized.getMessage());
        assertEquals("/api/execution/plan", deserialized.getPath());
        assertNotNull(deserialized.getTimestamp());
    }

    @Test
    void shouldHandleJsonPropertyAnnotations() throws Exception {
        // Given - проверяем, что JSON использует правильные имена полей
        ExecutionRequestDTO dto = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertTrue(json.contains("\"entity\":\"Building\""));
        assertTrue(json.contains("\"entityId\":\"93939\""));
        assertTrue(json.contains("\"action\":\"order_egrn_extract\""));
    }
}

