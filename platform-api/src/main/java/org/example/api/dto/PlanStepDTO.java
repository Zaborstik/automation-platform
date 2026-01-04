package org.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO для шага плана выполнения.
 */
public class PlanStepDTO {
    @JsonProperty("type")
    private String type;

    @JsonProperty("target")
    private String target;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    public PlanStepDTO() {
    }

    public PlanStepDTO(String type, String target, String explanation, Map<String, Object> parameters) {
        this.type = type;
        this.target = target;
        this.explanation = explanation;
        this.parameters = parameters;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}

