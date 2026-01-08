package com.zaborstik.platform.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Привязка действия к UI-элементу.
 * Содержит селекторы и метаданные для поиска элемента в UI.
 */
public record UIBinding(String actionId, String selector, SelectorType selectorType, Map<String, Object> metadata) {
    public enum SelectorType {
        CSS,
        XPATH,
        TEXT,
        ACTION_ID  // Специальный тип для семантических действий
    }

    public UIBinding(String actionId, String selector, SelectorType selectorType,
                     Map<String, Object> metadata) {
        this.actionId = Objects.requireNonNull(actionId, "Action id cannot be null");
        this.selector = Objects.requireNonNull(selector, "Selector cannot be null");
        this.selectorType = Objects.requireNonNull(selectorType, "Selector type cannot be null");
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UIBinding uiBinding = (UIBinding) o;
        return Objects.equals(actionId, uiBinding.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionId);
    }

    @Override
    public String toString() {
        return "UIBinding{actionId='" + actionId + "', selector='" + selector +
                "', type=" + selectorType + "}";
    }
}

