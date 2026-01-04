package org.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO для запроса на выполнение действия.
 * Пример:
 * {
 *   "entity": "Building",
 *   "entityId": "93939",
 *   "action": "order_egrn_extract",
 *   "parameters": {}
 * }
 */
public class ExecutionRequestDTO {
    @NotBlank(message = "Entity type is required")
    @JsonProperty("entity")
    private String entityType;

    @NotBlank(message = "Entity ID is required")
    @JsonProperty("entityId")
    private String entityId;

    @NotBlank(message = "Action is required")
    @JsonProperty("action")
    private String action;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    public ExecutionRequestDTO() {
    }

    public ExecutionRequestDTO(String entityType, String entityId, String action, 
                               Map<String, Object> parameters) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.parameters = parameters;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}

