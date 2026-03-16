package com.zaborstik.platform.knowledge.model;

import java.util.Map;
import java.util.Objects;

public record UIElement(
    String selector,
    String selectorType,
    String elementType,
    String label,
    Map<String, String> attributes
) {
    public UIElement(String selector, String selectorType, String elementType, String label, Map<String, String> attributes) {
        this.selector = Objects.requireNonNull(selector, "selector cannot be null");
        this.selectorType = selectorType;
        this.elementType = Objects.requireNonNull(elementType, "elementType cannot be null");
        this.label = label;
        this.attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
    }
}
