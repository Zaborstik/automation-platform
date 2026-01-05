package com.zaborstik.platform.agent.dto;

import java.util.Map;

/**
 * Ответ от агента после выполнения команды.
 */
public class AgentResponse {
    private final boolean success;
    private final String message;
    private final String error;
    private final Map<String, Object> data;
    private final long executionTimeMs;

    public AgentResponse(boolean success, String message, String error, 
                         Map<String, Object> data, long executionTimeMs) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.data = data != null ? Map.copyOf(data) : Map.of();
        this.executionTimeMs = executionTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public static AgentResponse success(String message, Map<String, Object> data, long executionTimeMs) {
        return new AgentResponse(true, message, null, data, executionTimeMs);
    }

    public static AgentResponse failure(String error, long executionTimeMs) {
        return new AgentResponse(false, null, error, Map.of(), executionTimeMs);
    }

    @Override
    public String toString() {
        return "AgentResponse{success=" + success + ", message='" + message + 
               "', error='" + error + "', executionTime=" + executionTimeMs + "ms}";
    }
}

