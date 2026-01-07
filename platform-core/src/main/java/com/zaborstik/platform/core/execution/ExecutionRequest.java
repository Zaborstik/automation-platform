package com.zaborstik.platform.core.execution;

import java.util.Map;
import java.util.Objects;

/**
 * Входные данные для выполнения действия.
 * Пример:
 * {
 *   "entity": "Building",
 *   "entityId": "93939",
 *   "action": "order_egrn_extract"
 * }
 * 
 * Input data for action execution.
 * Example:
 * {
 *   "entity": "Building",
 *   "entityId": "93939",
 *   "action": "order_egrn_extract"
 * }
 */
public class ExecutionRequest {
    private final String entityType;
    private final String entityId;
    private final String action;
    private final Map<String, Object> parameters;

    public ExecutionRequest(String entityType, String entityId, String action, 
                           Map<String, Object> parameters) {
        this.entityType = Objects.requireNonNull(entityType, "Entity type cannot be null");
        this.entityId = Objects.requireNonNull(entityId, "Entity id cannot be null");
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "ExecutionRequest{entityType='" + entityType + "', entityId='" + entityId + 
               "', action='" + action + "'}";
    }
}

