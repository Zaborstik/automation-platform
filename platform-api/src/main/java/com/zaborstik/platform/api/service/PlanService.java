package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.CreatePlanRequest;
import com.zaborstik.platform.api.dto.PlanResponse;
import com.zaborstik.platform.api.entity.*;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.*;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.plan.PlanStepAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис планов по новой логике: создание плана (после LLM/RAD), получение, запись результата и лога шага.
 */
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;
    private final PlanResultRepository planResultRepository;
    private final PlanStepLogEntryRepository planStepLogEntryRepository;
    private final ActionRepository actionRepository;
    private final AttachmentRepository attachmentRepository;

    public PlanService(PlanRepository planRepository,
                       PlanMapper planMapper,
                       PlanResultRepository planResultRepository,
                       PlanStepLogEntryRepository planStepLogEntryRepository,
                       ActionRepository actionRepository,
                       AttachmentRepository attachmentRepository) {
        this.planRepository = planRepository;
        this.planMapper = planMapper;
        this.planResultRepository = planResultRepository;
        this.planStepLogEntryRepository = planStepLogEntryRepository;
        this.actionRepository = actionRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request) {
        String planId = UUID.randomUUID().toString();
        List<PlanStep> steps = new ArrayList<>();
        for (int i = 0; i < request.getSteps().size(); i++) {
            CreatePlanRequest.PlanStepRequest sr = request.getSteps().get(i);
            String stepId = UUID.randomUUID().toString();
            List<PlanStepAction> actions = sr.getActions().stream()
                    .map(a -> new PlanStepAction(a.getActionId(), a.getMetaValue()))
                    .collect(Collectors.toList());
            steps.add(new PlanStep(
                    stepId,
                    planId,
                    sr.getWorkflowId(),
                    sr.getWorkflowStepInternalName(),
                    sr.getEntityTypeId(),
                    sr.getEntityId(),
                    sr.getSortOrder(),
                    sr.getDisplayName(),
                    actions
            ));
        }
        Plan plan = new Plan(
                planId,
                request.getWorkflowId(),
                request.getWorkflowStepInternalName(),
                steps.isEmpty() ? planId : steps.get(0).id(),
                request.getTarget(),
                request.getExplanation(),
                steps
        );
        PlanEntity entity = planMapper.toEntity(plan);
        planRepository.save(entity);
        return toResponse(planMapper.toDomain(entity));
    }

    @Transactional(readOnly = true)
    public Optional<PlanResponse> getPlan(String planId) {
        Objects.requireNonNull(planId, "planId");
        return planRepository.findById(planId)
                .map(planMapper::toDomain)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<Plan> getPlanDomain(String planId) {
        Objects.requireNonNull(planId, "planId");
        return planRepository.findById(planId).map(planMapper::toDomain);
    }

    /** Регистрация итога выполнения плана (после выполнения или прерывания). */
    @Transactional
    public PlanResultEntity createPlanResult(String planId, boolean success, Instant startedTime, Instant finishedTime) {
        Objects.requireNonNull(planId, "planId");
        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        PlanResultEntity result = new PlanResultEntity();
        result.setId(UUID.randomUUID().toString());
        result.setPlan(plan);
        result.setSuccess(success);
        result.setStartedTime(startedTime);
        result.setFinishedTime(finishedTime);
        return planResultRepository.save(result);
    }

    /** Запись лога по шагу (при падении/прерывании). */
    @Transactional
    public PlanStepLogEntryEntity createPlanStepLogEntry(String planId, String planStepId, String planResultId,
                                                         String actionId, String message, String error,
                                                         Instant executedTime, Long executionTimeMs, String attachmentId) {
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(planStepId, "planStepId");
        Objects.requireNonNull(planResultId, "planResultId");
        Objects.requireNonNull(actionId, "actionId");
        PlanEntity plan = planRepository.findById(planId).orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));
        PlanStepEntity planStep = plan.getSteps().stream().filter(s -> s.getId().equals(planStepId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plan step not found: " + planStepId));
        PlanResultEntity planResult = planResultRepository.findById(planResultId).orElseThrow(() -> new IllegalArgumentException("Plan result not found: " + planResultId));
        ActionEntity action = actionRepository.findById(actionId).orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));

        PlanStepLogEntryEntity entry = new PlanStepLogEntryEntity();
        entry.setId(UUID.randomUUID().toString());
        entry.setPlan(plan);
        entry.setPlanStep(planStep);
        entry.setPlanResult(planResult);
        entry.setAction(action);
        entry.setMessage(message);
        entry.setError(error);
        entry.setExecutedTime(executedTime);
        entry.setExecutionTimeMs(executionTimeMs);
        if (attachmentId != null) {
            attachmentRepository.findById(attachmentId).ifPresent(entry::setAttachment);
        }
        return planStepLogEntryRepository.save(entry);
    }

    @Transactional
    public AttachmentEntity createAttachment(String displayName) {
        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setId(UUID.randomUUID().toString());
        attachment.setDisplayname(displayName);
        return attachmentRepository.save(attachment);
    }

    private PlanResponse toResponse(Plan plan) {
        PlanResponse r = new PlanResponse();
        r.setId(plan.id());
        r.setWorkflowId(plan.workflowId());
        r.setWorkflowStepInternalName(plan.workflowStepInternalName());
        r.setStoppedAtPlanStepId(plan.stoppedAtPlanStepId());
        r.setTarget(plan.target());
        r.setExplanation(plan.explanation());
        r.setSteps(plan.steps().stream().map(this::toStepResponse).collect(Collectors.toList()));
        return r;
    }

    private PlanResponse.PlanStepResponse toStepResponse(PlanStep s) {
        PlanResponse.PlanStepResponse r = new PlanResponse.PlanStepResponse();
        r.setId(s.id());
        r.setWorkflowId(s.workflowId());
        r.setWorkflowStepInternalName(s.workflowStepInternalName());
        r.setEntityTypeId(s.entityTypeId());
        r.setEntityId(s.entityId());
        r.setSortOrder(s.sortOrder());
        r.setDisplayName(s.displayName());
        r.setActions(s.actions().stream().map(a -> {
            PlanResponse.PlanStepActionResponse ar = new PlanResponse.PlanStepActionResponse();
            ar.setActionId(a.actionId());
            ar.setMetaValue(a.metaValue());
            return ar;
        }).collect(Collectors.toList()));
        return r;
    }
}
