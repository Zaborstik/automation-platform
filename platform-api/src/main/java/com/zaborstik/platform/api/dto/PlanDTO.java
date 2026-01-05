package com.zaborstik.platform.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для плана выполнения действия.
 */
public class PlanDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("entityType")
    private String entityTypeId;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("action")
    private String actionId;

    @JsonProperty("steps")
    private List<PlanStepDTO> steps;

    @JsonProperty("status")
    private String status;

    public PlanDTO() {
    }

    public PlanDTO(String id, String entityTypeId, String entityId, String actionId, 
                   List<PlanStepDTO> steps, String status) {
        this.id = id;
        this.entityTypeId = entityTypeId;
        this.entityId = entityId;
        this.actionId = actionId;
        this.steps = steps;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntityTypeId() {
        return entityTypeId;
    }

    public void setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public List<PlanStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStepDTO> steps) {
        this.steps = steps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

