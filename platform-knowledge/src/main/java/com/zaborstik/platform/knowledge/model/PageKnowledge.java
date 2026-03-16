package com.zaborstik.platform.knowledge.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PageKnowledge(
    String pageUrl,
    String pageTitle,
    List<UIElement> elements,
    Instant scannedAt
) {
    public PageKnowledge(String pageUrl, String pageTitle, List<UIElement> elements, Instant scannedAt) {
        this.pageUrl = Objects.requireNonNull(pageUrl, "pageUrl cannot be null");
        this.pageTitle = pageTitle;
        this.elements = elements != null ? List.copyOf(elements) : List.of();
        this.scannedAt = scannedAt;
    }
}
