package com.zaborstik.platform.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Запрос на создание плана (после обработки LLM/RAD).
 */
public class CreatePlanRequest {

    @NotBlank
    private String workflowId;
    @NotBlank
    private String workflowStepInternalName;
    /** ID шага плана, на котором остановилось выполнение (опционально при создании — подставится первый шаг). */
    private String stoppedAtPlanStepId;
    private String target;
    private String explanation;
    @Valid
    private List<PlanStepRequest> steps = List.of();

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getWorkflowStepInternalName() { return workflowStepInternalName; }
    public void setWorkflowStepInternalName(String workflowStepInternalName) { this.workflowStepInternalName = workflowStepInternalName; }
    public String getStoppedAtPlanStepId() { return stoppedAtPlanStepId; }
    public void setStoppedAtPlanStepId(String stoppedAtPlanStepId) { this.stoppedAtPlanStepId = stoppedAtPlanStepId; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<PlanStepRequest> getSteps() { return steps; }
    public void setSteps(List<PlanStepRequest> steps) { this.steps = steps != null ? steps : List.of(); }

    public static class PlanStepRequest {
        @NotBlank
        private String workflowId;
        @NotBlank
        private String workflowStepInternalName;
        @NotBlank
        private String entityTypeId;
        private String entityId;
        private int sortOrder;
        @NotBlank
        private String displayName;
        @Valid
        private List<PlanStepActionRequest> actions = List.of();

        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        public String getWorkflowStepInternalName() { return workflowStepInternalName; }
        public void setWorkflowStepInternalName(String s) { this.workflowStepInternalName = s; }
        public String getEntityTypeId() { return entityTypeId; }
        public void setEntityTypeId(String entityTypeId) { this.entityTypeId = entityTypeId; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public List<PlanStepActionRequest> getActions() { return actions; }
        public void setActions(List<PlanStepActionRequest> actions) { this.actions = actions != null ? actions : List.of(); }
    }

    public static class PlanStepActionRequest {
        @NotBlank
        private String actionId;
        private String metaValue;

        public String getActionId() { return actionId; }
        public void setActionId(String actionId) { this.actionId = actionId; }
        public String getMetaValue() { return metaValue; }
        public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
    }
}
