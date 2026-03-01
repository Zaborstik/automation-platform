package com.zaborstik.platform.core.domain;

import java.util.Objects;

/**
 * Шаг жизненного цикла (system.workflow_step).
 * Универсальные шаги ЖЦ: new, in_progress, paused, completed, failed, cancelled.
 */
public record WorkflowStep(String id, String internalName, String displayName, Integer sortOrder) {
    public WorkflowStep(String id, String internalName, String displayName, Integer sortOrder) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.internalName = Objects.requireNonNull(internalName, "internalName cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.sortOrder = sortOrder;
    }
}
