package com.zaborstik.platform.executor.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Local mirror of {@code com.zaborstik.platform.api.dto.PlanResponse} used by
 * the executor when calling {@code GET /api/plans/{id}} and {@code POST
 * /api/plans/from-request}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanDto {

    public String id;
    public String workflowId;
    public String workflowStepInternalName;
    public String stoppedAtPlanStepId;
    public String target;
    public String explanation;
    public List<PlanStepDto> steps;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanStepDto {
        public String id;
        public String workflowId;
        public String workflowStepInternalName;
        public String entityTypeId;
        public String entityId;
        public int sortOrder;
        public String displayName;
        public List<PlanStepActionDto> actions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanStepActionDto {
        public String actionId;
        public String metaValue;
    }
}
