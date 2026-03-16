package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

public class TransitionPlanRequest {

    @NotBlank
    private String targetStep;

    public String getTargetStep() { return targetStep; }
    public void setTargetStep(String targetStep) { this.targetStep = targetStep; }
}
