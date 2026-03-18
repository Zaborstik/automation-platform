package com.zaborstik.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на создание плана из пользовательского текстового запроса (LLM-парсинг).
 */
public class CreatePlanFromRequestRequest {

    @NotBlank(message = "userInput is required")
    private String userInput;

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }
}
