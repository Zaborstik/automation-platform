package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.domain.WorkflowStep;
import com.zaborstik.platform.core.domain.WorkflowTransition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory реализация Resolver.
 * Хранит entity_type, action_type, action, workflow, workflow_step и связку action_applicable_entity_type.
 */
public class InMemoryResolver implements Resolver {

    private final ConcurrentHashMap<String, EntityType> entityTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Action> actions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ActionType> actionTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Workflow> workflows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WorkflowStep> workflowSteps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<WorkflowTransition>> transitions = new ConcurrentHashMap<>();
    /** (actionId, entityTypeId) */
    private final Set<ActionEntityTypeKey> applicable = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, UIBinding> uiBindings = new ConcurrentHashMap<>();

    public void registerEntityType(EntityType entityType) {
        entityTypes.put(entityType.id(), entityType);
    }

    public void registerAction(Action action) {
        actions.put(action.id(), action);
    }

    public void registerActionType(ActionType actionType) {
        actionTypes.put(actionType.id(), actionType);
    }

    public void registerWorkflow(Workflow workflow) {
        workflows.put(workflow.id(), workflow);
    }

    public void registerWorkflowStep(WorkflowStep step) {
        workflowSteps.put(step.id(), step);
    }

    public void registerTransition(WorkflowTransition transition) {
        Objects.requireNonNull(transition, "transition cannot be null");
        transitions.computeIfAbsent(transition.workflowId(), key -> new CopyOnWriteArrayList<>())
            .add(transition);
    }

    /** Регистрация применимости действия к типу сущности (action_applicable_entity_type). */
    public void registerActionApplicableToEntityType(String actionId, String entityTypeId) {
        applicable.add(new ActionEntityTypeKey(actionId, entityTypeId));
    }

    public void registerUIBinding(UIBinding uiBinding) {
        uiBindings.put(uiBinding.actionId(), uiBinding);
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return Optional.ofNullable(entityTypes.get(entityTypeId));
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return Optional.ofNullable(actions.get(actionId));
    }

    @Override
    public Optional<ActionType> findActionType(String actionTypeId) {
        return Optional.ofNullable(actionTypes.get(actionTypeId));
    }

    @Override
    public Optional<Workflow> findWorkflow(String workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStep(String workflowStepId) {
        return Optional.ofNullable(workflowSteps.get(workflowStepId));
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStepByInternalName(String internalName) {
        return workflowSteps.values().stream()
            .filter(s -> s.internalName().equals(internalName))
            .findFirst();
    }

    @Override
    public List<WorkflowTransition> findTransitions(String workflowId) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        return List.copyOf(transitions.getOrDefault(workflowId, List.of()));
    }

    @Override
    public Optional<WorkflowTransition> findTransition(String workflowId, String fromStep, String toStep) {
        Objects.requireNonNull(workflowId, "workflowId cannot be null");
        Objects.requireNonNull(fromStep, "fromStep cannot be null");
        Objects.requireNonNull(toStep, "toStep cannot be null");
        return findTransitions(workflowId).stream()
            .filter(transition -> transition.fromStepInternalName().equals(fromStep))
            .filter(transition -> transition.toStepInternalName().equals(toStep))
            .findFirst();
    }

    @Override
    public List<Action> findActionsApplicableToEntityType(String entityTypeId) {
        List<Action> result = new ArrayList<>();
        for (ActionEntityTypeKey key : applicable) {
            if (key.entityTypeId.equals(entityTypeId)) {
                findAction(key.actionId).ifPresent(result::add);
            }
        }
        return result;
    }

    @Override
    public boolean isActionApplicable(String actionId, String entityTypeId) {
        return applicable.contains(new ActionEntityTypeKey(actionId, entityTypeId));
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return Optional.ofNullable(uiBindings.get(actionId));
    }

    private record ActionEntityTypeKey(String actionId, String entityTypeId) {}
}
