package com.zaborstik.platform.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/knowledge/generate-plan}.
 *
 * <p>Carries a single free-form user request. The knowledge service is the
 * source of truth for translating the request into a {@link com.zaborstik.platform.core.plan.Plan}.
 */
public class GeneratePlanRequest {

    @NotBlank
    private String userInput;

    public GeneratePlanRequest() {
    }

    public GeneratePlanRequest(String userInput) {
        this.userInput = userInput;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}
