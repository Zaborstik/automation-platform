package com.zaborstik.platform.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Привязка действия к UI-элементу.
 * Содержит elementName (семантическое имя) и/или selector для поиска элемента в UI.
 * elementName имеет приоритет при resolution; selector используется как fallback.
 */
public record UIBinding(String actionId, String elementName, String selector, SelectorType selectorType, Map<String, Object> metadata) {
    public enum SelectorType {
        CSS,
        XPATH,
        TEXT,
        ACTION_ID  // Специальный тип для семантических действий
    }

    public UIBinding(String actionId, String elementName, String selector, SelectorType selectorType,
                     Map<String, Object> metadata) {
        this.actionId = Objects.requireNonNull(actionId, "Action id cannot be null");
        this.elementName = elementName;
        this.selector = selector;
        this.selectorType = (selector != null && !selector.isBlank())
            ? Objects.requireNonNull(selectorType, "Selector type cannot be null when selector is provided")
            : (selectorType != null ? selectorType : SelectorType.CSS);
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        if (this.elementName == null && (this.selector == null || this.selector.isBlank())) {
            throw new IllegalArgumentException("Either elementName or selector must be non-null/non-blank");
        }
    }

    /**
     * Конструктор для обратной совместимости: (actionId, selector, selectorType, metadata).
     */
    public UIBinding(String actionId, String selector, SelectorType selectorType, Map<String, Object> metadata) {
        this(actionId, null, Objects.requireNonNull(selector, "Selector cannot be null"), selectorType, metadata);
    }

    /** Возвращает селектор для использования (selector как fallback, если elementName не разрешён). */
    public String effectiveSelector() {
        return selector != null && !selector.isBlank() ? selector : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UIBinding uiBinding)) return false;
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

