package com.zaborstik.platform.executor.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /local/run}. The chat overlay forwards what the user
 * typed as {@link #userInput}; the optional fields override the defaults
 * configured via {@code platform.agent.base-url} and
 * {@code platform.agent.headless}.
 */
public class LocalRunRequest {

    @NotBlank
    private String userInput;
    private String browserBaseUrl;
    private Boolean headless;

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }
    public String getBrowserBaseUrl() { return browserBaseUrl; }
    public void setBrowserBaseUrl(String browserBaseUrl) { this.browserBaseUrl = browserBaseUrl; }
    public Boolean getHeadless() { return headless; }
    public void setHeadless(Boolean headless) { this.headless = headless; }
}
