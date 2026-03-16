package com.zaborstik.platform.core.domain;

import java.util.Objects;

/**
 * Допустимый переход жизненного цикла (system.workflow_transition).
 */
public record WorkflowTransition(
    String workflowId,
    String fromStepInternalName,
    String toStepInternalName
) {
    public WorkflowTransition {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(fromStepInternalName, "fromStepInternalName cannot be null");
        Objects.requireNonNull(toStepInternalName, "toStepInternalName cannot be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorkflowTransition other)) {
            return false;
        }
        return workflowId.equals(other.workflowId)
            && fromStepInternalName.equals(other.fromStepInternalName)
            && toStepInternalName.equals(other.toStepInternalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowId, fromStepInternalName, toStepInternalName);
    }

    @Override
    public String toString() {
        return "WorkflowTransition[" +
            "workflowId=" + workflowId +
            ", fromStepInternalName=" + fromStepInternalName +
            ", toStepInternalName=" + toStepInternalName +
            "]";
    }
}
