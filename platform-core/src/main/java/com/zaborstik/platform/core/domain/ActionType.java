package com.zaborstik.platform.core.domain;

import java.util.Objects;

/**
 * Тип действия (system.action_type).
 * Типы действий в RAD/LLM-среде: navigation, interaction, data_input, validation, artifact.
 */
public record ActionType(String id, String internalName, String displayName) {
    public ActionType(String id, String internalName, String displayName) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.internalName = Objects.requireNonNull(internalName, "internalName cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
    }
}
