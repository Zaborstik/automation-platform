package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/plans/from-request}.
 *
 * <p>The platform-api forwards {@link #userInput} to {@code platform-knowledge}
 * over HTTP, persists the returned plan and answers with {@link PlanResponse}.
 */
public class CreatePlanFromRequestRequest {

    @NotBlank
    private String userInput;

    public CreatePlanFromRequestRequest() {
    }

    public CreatePlanFromRequestRequest(String userInput) {
        this.userInput = userInput;
    }

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }
}
