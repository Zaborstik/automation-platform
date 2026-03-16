package com.zaborstik.platform.core.lifecycle;

import com.zaborstik.platform.core.resolver.Resolver;

import java.util.Objects;

/**
 * Управление переходами жизненного цикла.
 */
public class LifecycleManager {

    private final Resolver resolver;

    public LifecycleManager(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public boolean canTransition(String workflowId, String currentStep, String targetStep) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(currentStep, "currentStep cannot be null");
        Objects.requireNonNull(targetStep, "targetStep cannot be null");
        return resolver.findTransition(workflowId, currentStep, targetStep).isPresent();
    }

    public void validateTransition(String workflowId, String currentStep, String targetStep) {
        if (!canTransition(workflowId, currentStep, targetStep)) {
            throw new IllegalStateException(
                "Transition is not allowed for workflow '" + workflowId + "': "
                    + currentStep + " -> " + targetStep
            );
        }
    }

    public String getNextStep(String workflowId, String currentStep) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(currentStep, "currentStep cannot be null");
        return resolver.findTransitions(workflowId).stream()
            .filter(transition -> transition.fromStepInternalName().equals(currentStep))
            .map(transition -> transition.toStepInternalName())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No outgoing transition for workflow '" + workflowId + "' from step '" + currentStep + "'"
            ));
    }
}
