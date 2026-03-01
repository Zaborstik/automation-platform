package com.zaborstik.platform.core.domain;

import java.util.Objects;

/**
 * Жизненный цикл (system.workflow).
 * Например: ЖЦ плана (wf-plan), ЖЦ шага плана (wf-plan-step).
 */
public record Workflow(String id, String displayName, String firstStepId) {
    public Workflow(String id, String displayName, String firstStepId) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.firstStepId = Objects.requireNonNull(firstStepId, "firstStepId cannot be null");
    }
}
