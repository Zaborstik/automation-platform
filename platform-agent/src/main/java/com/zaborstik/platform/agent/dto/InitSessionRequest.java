package com.zaborstik.platform.agent.dto;

/**
 * Body of {@code POST /api/agent/session}. {@code baseUrl} is what the
 * Playwright sidecar should treat as the default origin for the next page,
 * {@code headless} controls whether the browser window is visible.
 */
public class InitSessionRequest {

    private String baseUrl;
    private Boolean headless;

    public InitSessionRequest() {
    }

    public InitSessionRequest(String baseUrl, Boolean headless) {
        this.baseUrl = baseUrl;
        this.headless = headless;
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Boolean getHeadless() { return headless; }
    public void setHeadless(Boolean headless) { this.headless = headless; }
}
