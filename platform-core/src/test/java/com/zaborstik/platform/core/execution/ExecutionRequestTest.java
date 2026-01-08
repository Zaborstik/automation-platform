package com.zaborstik.platform.core.execution;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionRequestTest {

    @Test
    void shouldCreateExecutionRequestWithAllFields() {
        Map<String, Object> parameters = Map.of("param1", "value1", "param2", 123);
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            parameters
        );

        assertEquals("Building", request.entityType());
        assertEquals("93939", request.entityId());
        assertEquals("order_egrn_extract", request.action());
        assertEquals(parameters, request.parameters());
    }

    @Test
    void shouldCreateExecutionRequestWithNullParameters() {
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            null
        );

        assertEquals("Building", request.entityType());
        assertTrue(request.parameters().isEmpty());
    }

    @Test
    void shouldCreateExecutionRequestWithEmptyParameters() {
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        assertEquals("Building", request.entityType());
        assertTrue(request.parameters().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenEntityTypeIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new ExecutionRequest(null, "93939", "order_egrn_extract", Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenEntityIdIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new ExecutionRequest("Building", null, "order_egrn_extract", Map.of());
        });
    }

    @Test
    void shouldThrowExceptionWhenActionIsNull() {
        assertThrows(NullPointerException.class, () -> {
            new ExecutionRequest("Building", "93939", null, Map.of());
        });
    }

    @Test
    void shouldReturnImmutableParameters() {
        Map<String, Object> originalParams = Map.of("key", "value");
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            originalParams
        );

        Map<String, Object> returnedParams = request.parameters();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedParams.put("newKey", "newValue");
        });
    }

    @Test
    void shouldReturnCorrectToString() {
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );
        String toString = request.toString();

        assertTrue(toString.contains("Building"));
        assertTrue(toString.contains("93939"));
        assertTrue(toString.contains("order_egrn_extract"));
        assertTrue(toString.contains("ExecutionRequest"));
    }
}

