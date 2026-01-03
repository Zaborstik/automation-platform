package org.example.core.plan;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * План выполнения действия.
 * План - это данные, не код. Хранится в БД, сериализуется, редактируется, генерируется ИИ.
 */
public class Plan {
    private final String id;
    private final String entityTypeId;
    private final String entityId;
    private final String actionId;
    private final List<PlanStep> steps;
    private final PlanStatus status;

    public enum PlanStatus {
        CREATED,
        EXECUTING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public Plan(String entityTypeId, String entityId, String actionId, List<PlanStep> steps) {
        this.id = UUID.randomUUID().toString();
        this.entityTypeId = Objects.requireNonNull(entityTypeId, "EntityType id cannot be null");
        this.entityId = Objects.requireNonNull(entityId, "Entity id cannot be null");
        this.actionId = Objects.requireNonNull(actionId, "Action id cannot be null");
        this.steps = steps != null ? List.copyOf(steps) : List.of();
        this.status = PlanStatus.CREATED;
    }

    public Plan(String id, String entityTypeId, String entityId, String actionId, 
                List<PlanStep> steps, PlanStatus status) {
        this.id = Objects.requireNonNull(id, "Plan id cannot be null");
        this.entityTypeId = Objects.requireNonNull(entityTypeId, "EntityType id cannot be null");
        this.entityId = Objects.requireNonNull(entityId, "Entity id cannot be null");
        this.actionId = Objects.requireNonNull(actionId, "Action id cannot be null");
        this.steps = steps != null ? List.copyOf(steps) : List.of();
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getEntityTypeId() {
        return entityTypeId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getActionId() {
        return actionId;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public Plan withStatus(PlanStatus newStatus) {
        return new Plan(id, entityTypeId, entityId, actionId, steps, newStatus);
    }

    @Override
    public String toString() {
        return "Plan{id='" + id + "', entityType='" + entityTypeId + 
               "', entityId='" + entityId + "', action='" + actionId + 
               "', steps=" + steps.size() + ", status=" + status + "}";
    }
}

