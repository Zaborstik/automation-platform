package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.domain.WorkflowStep;
import com.zaborstik.platform.core.domain.WorkflowTransition;

import java.util.List;
import java.util.Optional;

/**
 * Resolver для поиска сущностей по данным БД.
 * Применимость действий к типам сущностей задаётся таблицей action_applicable_entity_type.
 */
public interface Resolver {

    Optional<EntityType> findEntityType(String entityTypeId);

    Optional<Action> findAction(String actionId);

    Optional<ActionType> findActionType(String actionTypeId);

    Optional<Workflow> findWorkflow(String workflowId);

    Optional<WorkflowStep> findWorkflowStep(String workflowStepId);

    Optional<WorkflowStep> findWorkflowStepByInternalName(String internalName);

    List<WorkflowTransition> findTransitions(String workflowId);

    Optional<WorkflowTransition> findTransition(String workflowId, String fromStep, String toStep);

    /**
     * Действия, применимые к данному типу сущности (action_applicable_entity_type).
     */
    List<Action> findActionsApplicableToEntityType(String entityTypeId);

    boolean isActionApplicable(String actionId, String entityTypeId);

    /**
     * Привязка действия к UI (опционально, для executor).
     */
    Optional<UIBinding> findUIBinding(String actionId);

    /**
     * true, если {@code internalName} совпадает с {@code internalname} какой-либо записи {@code system.workflow_step}
     * (элемент справочника шагов ЖЦ).
     */
    boolean isWorkflowStepInternalName(String internalName);
}
