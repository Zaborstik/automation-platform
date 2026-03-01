package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.entity.*;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Маппер Plan ↔ PlanEntity (zbrtstk.plan, plan_step, plan_step_action).
 */
@Component
public class PlanMapper {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final EntityTypeRepository entityTypeRepository;
    private final ActionRepository actionRepository;

    public PlanMapper(WorkflowRepository workflowRepository,
                      WorkflowStepRepository workflowStepRepository,
                      EntityTypeRepository entityTypeRepository,
                      ActionRepository actionRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.entityTypeRepository = entityTypeRepository;
        this.actionRepository = actionRepository;
    }

    public PlanEntity toEntity(Plan plan) {
        PlanEntity pe = new PlanEntity();
        pe.setId(plan.id());
        workflowRepository.findById(plan.workflowId()).ifPresent(pe::setWorkflow);
        pe.setWorkflowStepInternalname(plan.workflowStepInternalName());
        pe.setStoppedAtPlanStep(plan.stoppedAtPlanStepId());
        pe.setTarget(plan.target());
        pe.setExplanation(plan.explanation());

        List<PlanStepEntity> stepEntities = new ArrayList<>();
        for (int i = 0; i < plan.steps().size(); i++) {
            stepEntities.add(toStepEntity(plan.steps().get(i), pe));
        }
        pe.setSteps(stepEntities);
        return pe;
    }

    public Plan toDomain(PlanEntity e) {
        List<PlanStep> steps = e.getSteps().stream()
                .map(this::toStep)
                .collect(Collectors.toList());
        return new Plan(
                e.getId(),
                e.getWorkflow() != null ? e.getWorkflow().getId() : null,
                e.getWorkflowStepInternalname(),
                e.getStoppedAtPlanStep(),
                e.getTarget(),
                e.getExplanation(),
                steps
        );
    }

    private PlanStepEntity toStepEntity(PlanStep step, PlanEntity plan) {
        PlanStepEntity pse = new PlanStepEntity();
        pse.setId(step.id());
        pse.setPlan(plan);
        workflowRepository.findById(step.workflowId()).ifPresent(pse::setWorkflow);
        pse.setWorkflowStepInternalname(step.workflowStepInternalName());
        entityTypeRepository.findById(step.entityTypeId()).ifPresent(pse::setEntitytype);
        pse.setEntityId(step.entityId());
        pse.setSortorder(step.sortOrder());
        pse.setDisplayname(step.displayName());

        List<PlanStepActionEntity> actionEntities = new ArrayList<>();
        for (PlanStepAction a : step.actions()) {
            PlanStepActionEntity pae = new PlanStepActionEntity();
            pae.setPlanStep(pse);
            actionRepository.findById(a.actionId()).ifPresent(pae::setAction);
            pae.setMetaValue(a.metaValue());
            actionEntities.add(pae);
        }
        pse.setActions(actionEntities);
        return pse;
    }

    private PlanStep toStep(PlanStepEntity e) {
        List<PlanStepAction> actions = e.getActions().stream()
                .map(a -> new PlanStepAction(
                        a.getAction() != null ? a.getAction().getId() : null,
                        a.getMetaValue()))
                .collect(Collectors.toList());
        return new PlanStep(
                e.getId(),
                e.getPlan() != null ? e.getPlan().getId() : null,
                e.getWorkflow() != null ? e.getWorkflow().getId() : null,
                e.getWorkflowStepInternalname(),
                e.getEntitytype() != null ? e.getEntitytype().getId() : null,
                e.getEntityId(),
                e.getSortorder(),
                e.getDisplayname(),
                actions
        );
    }
}
