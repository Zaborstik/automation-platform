package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.entity.*;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.ActionType;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.domain.Workflow;
import com.zaborstik.platform.core.domain.WorkflowStep;
import com.zaborstik.platform.core.domain.WorkflowTransition;
import com.zaborstik.platform.core.resolver.Resolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolver по БД (V1): entity_type, action_type, action, action_applicable_entity_type, workflow, workflow_step.
 * UI binding в новой схеме нет — findUIBinding возвращает empty.
 */
@Component
public class DatabaseResolver implements Resolver {

    private final EntityTypeRepository entityTypeRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final ActionRepository actionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    /** Lazy cache: все {@code workflow_step.internalname} из БД. */
    private volatile Set<String> workflowStepInternalNamesCache;

    public DatabaseResolver(EntityTypeRepository entityTypeRepository,
                            ActionTypeRepository actionTypeRepository,
                            ActionRepository actionRepository,
                            WorkflowRepository workflowRepository,
                            WorkflowStepRepository workflowStepRepository,
                            WorkflowTransitionRepository workflowTransitionRepository) {
        this.entityTypeRepository = entityTypeRepository;
        this.actionTypeRepository = actionTypeRepository;
        this.actionRepository = actionRepository;
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return entityTypeRepository.findById(entityTypeId).map(this::toEntityType);
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return actionRepository.findById(actionId).map(this::toAction);
    }

    @Override
    public Optional<ActionType> findActionType(String actionTypeId) {
        return actionTypeRepository.findById(actionTypeId).map(this::toActionType);
    }

    @Override
    public Optional<Workflow> findWorkflow(String workflowId) {
        return workflowRepository.findById(workflowId).map(this::toWorkflow);
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStep(String workflowStepId) {
        return workflowStepRepository.findById(workflowStepId).map(this::toWorkflowStep);
    }

    @Override
    public Optional<WorkflowStep> findWorkflowStepByInternalName(String internalName) {
        return workflowStepRepository.findByInternalname(internalName).map(this::toWorkflowStep);
    }

    @Override
    public List<Action> findActionsApplicableToEntityType(String entityTypeId) {
        return actionRepository.findByApplicableEntityTypes_Id(entityTypeId).stream()
                .map(this::toAction)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isActionApplicable(String actionId, String entityTypeId) {
        return findActionsApplicableToEntityType(entityTypeId).stream()
                .anyMatch(a -> a.id().equals(actionId));
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return Optional.empty();
    }

    @Override
    public boolean isWorkflowStepInternalName(String internalName) {
        if (internalName == null || internalName.isBlank()) {
            return false;
        }
        Set<String> cache = workflowStepInternalNamesCache;
        if (cache == null) {
            synchronized (this) {
                if (workflowStepInternalNamesCache == null) {
                    workflowStepInternalNamesCache = workflowStepRepository.findAll().stream()
                        .map(WorkflowStepEntity::getInternalname)
                        .collect(Collectors.toUnmodifiableSet());
                }
                cache = workflowStepInternalNamesCache;
            }
        }
        return cache.contains(internalName);
    }

    @Override
    public List<WorkflowTransition> findTransitions(String workflowId) {
        return workflowTransitionRepository.findByWorkflow_Id(workflowId).stream()
                .map(this::toWorkflowTransition)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WorkflowTransition> findTransition(String workflowId, String fromStep, String toStep) {
        return workflowTransitionRepository.findByWorkflow_IdAndFromStepAndToStep(workflowId, fromStep, toStep)
                .map(this::toWorkflowTransition);
    }

    private EntityType toEntityType(EntityTypeEntity e) {
        return new EntityType(
                e.getId(),
                e.getDisplayname(),
                e.getCreatedTime(),
                e.getUpdatedTime(),
                e.getKmArticle(),
                e.getUiDescription(),
                e.getEntityfieldlist(),
                e.getButtons()
        );
    }

    private Action toAction(ActionEntity e) {
        return new Action(
                e.getId(),
                e.getDisplayname(),
                e.getInternalname(),
                e.getMetaValue(),
                e.getDescription(),
                e.getActionType() != null ? e.getActionType().getId() : null,
                e.getCreatedTime(),
                e.getUpdatedTime()
        );
    }

    private ActionType toActionType(ActionTypeEntity e) {
        return new ActionType(e.getId(), e.getInternalname(), e.getDisplayname());
    }

    private Workflow toWorkflow(WorkflowEntity e) {
        String firstStepId = e.getFirststep() != null ? e.getFirststep().getId() : null;
        return new Workflow(e.getId(), e.getDisplayname(), firstStepId != null ? firstStepId : "");
    }

    private WorkflowStep toWorkflowStep(WorkflowStepEntity e) {
        return new WorkflowStep(e.getId(), e.getInternalname(), e.getDisplayname(), e.getSortorder());
    }

    private WorkflowTransition toWorkflowTransition(WorkflowTransitionEntity e) {
        String wfId = e.getWorkflow() != null ? e.getWorkflow().getId() : null;
        return new WorkflowTransition(wfId, e.getFromStep(), e.getToStep());
    }
}
