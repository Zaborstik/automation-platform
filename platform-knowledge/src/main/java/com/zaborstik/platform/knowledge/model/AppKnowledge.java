package com.zaborstik.platform.knowledge.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AppKnowledge(
    String appId,
    String appName,
    String baseUrl,
    List<PageKnowledge> pages,
    Instant discoveredAt
) {
    public AppKnowledge(String appId, String appName, String baseUrl, List<PageKnowledge> pages, Instant discoveredAt) {
        this.appId = Objects.requireNonNull(appId, "appId cannot be null");
        this.appName = Objects.requireNonNull(appName, "appName cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        this.pages = pages != null ? List.copyOf(pages) : List.of();
        this.discoveredAt = discoveredAt;
    }
}
