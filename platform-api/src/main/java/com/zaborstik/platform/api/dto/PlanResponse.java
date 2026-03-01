package com.zaborstik.platform.api.dto;

import java.util.List;

/**
 * Ответ с планом (для GET /api/plans/{id}).
 */
public class PlanResponse {

    private String id;
    private String workflowId;
    private String workflowStepInternalName;
    private String stoppedAtPlanStepId;
    private String target;
    private String explanation;
    private List<PlanStepResponse> steps;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getWorkflowStepInternalName() { return workflowStepInternalName; }
    public void setWorkflowStepInternalName(String s) { this.workflowStepInternalName = s; }
    public String getStoppedAtPlanStepId() { return stoppedAtPlanStepId; }
    public void setStoppedAtPlanStepId(String stoppedAtPlanStepId) { this.stoppedAtPlanStepId = stoppedAtPlanStepId; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<PlanStepResponse> getSteps() { return steps; }
    public void setSteps(List<PlanStepResponse> steps) { this.steps = steps; }

    public static class PlanStepResponse {
        private String id;
        private String workflowId;
        private String workflowStepInternalName;
        private String entityTypeId;
        private String entityId;
        private int sortOrder;
        private String displayName;
        private List<PlanStepActionResponse> actions;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
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
        public List<PlanStepActionResponse> getActions() { return actions; }
        public void setActions(List<PlanStepActionResponse> actions) { this.actions = actions; }
    }

    public static class PlanStepActionResponse {
        private String actionId;
        private String metaValue;

        public String getActionId() { return actionId; }
        public void setActionId(String actionId) { this.actionId = actionId; }
        public String getMetaValue() { return metaValue; }
        public void setMetaValue(String metaValue) { this.metaValue = metaValue; }
    }
}
