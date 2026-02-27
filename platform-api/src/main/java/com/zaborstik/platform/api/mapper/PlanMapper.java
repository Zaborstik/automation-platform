package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.PlanStepEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Маппер Plan ↔ PlanEntity/PlanStepEntity (схема system) и Plan ↔ EntityDTO (API).
 */
@Component
public class PlanMapper {

    private final ActionRepository actionRepository;

    public PlanMapper(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    /** Plan → PlanEntity для сохранения в БД. */
    public PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.setShortname(plan.id());
        entity.setEntityTypeId(plan.entityTypeId());
        entity.setEntityId(plan.entityId());
        entity.setStatus(convertStatus(plan.status()));
        actionRepository.findById(plan.actionId()).ifPresent(entity::setAction);

        List<PlanStepEntity> stepEntities = new ArrayList<>();
        for (int i = 0; i < plan.steps().size(); i++) {
            stepEntities.add(toStepEntity(i, plan.steps().get(i)));
        }
        entity.setSteps(stepEntities);
        return entity;
    }

    /** PlanEntity → Plan. */
    public Plan toDomain(PlanEntity entity) {
        List<PlanStep> steps = entity.getSteps().stream()
                .map(this::toStep)
                .collect(Collectors.toList());
        return new Plan(
                entity.getId(),
                entity.getEntityTypeId(),
                entity.getEntityId(),
                entity.getActionId(),
                steps,
                convertStatus(entity.getStatus())
        );
    }

    /** Plan → EntityDTO для ответа API. */
    public EntityDTO toEntityDTO(Plan plan) {
        List<Map<String, Object>> stepsData = plan.steps().stream()
                .map(this::stepToMap)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>(Map.of(
                "entityTypeId", plan.entityTypeId(),
                "entityId", plan.entityId(),
                "actionId", plan.actionId(),
                "status", plan.status().name(),
                "steps", stepsData
        ));
        return new EntityDTO(EntityDTO.TABLE_PLANS, plan.id(), data);
    }

    /** EntityDTO (request) → Plan для создания. */
    public Plan toPlan(EntityDTO dto) {
        if (dto == null || !EntityDTO.TABLE_PLANS.equals(dto.getTableName())) {
            throw new IllegalArgumentException("Expected EntityDTO with tableName=plans");
        }
        String id = dto.getId();
        String entityTypeId = (String) dto.getData().get("entityTypeId");
        String entityId = (String) dto.getData().get("entityId");
        String actionId = (String) dto.getData().get("actionId");
        String statusStr = (String) dto.getData().get("status");
        Plan.PlanStatus status = parseStatus(statusStr);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stepsData = (List<Map<String, Object>>) dto.getData().get("steps");
        List<PlanStep> steps = stepsData != null
                ? stepsData.stream().map(this::mapToStep).collect(Collectors.toList())
                : List.of();
        return new Plan(id, entityTypeId, entityId, actionId, steps, status);
    }

    private PlanStepEntity toStepEntity(int index, PlanStep step) {
        PlanStepEntity se = new PlanStepEntity();
        se.setShortname(UUID.randomUUID().toString());
        se.setSortorder(index);
        se.setStepType(step.type());
        se.setTarget(step.target());
        se.setExplanation(step.explanation());
        se.setParameters(step.parameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() != null ? e.getValue().toString() : "")));
        return se;
    }

    private PlanStep toStep(PlanStepEntity e) {
        Map<String, Object> params = e.getParameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new PlanStep(e.getType(), e.getTarget(), e.getExplanation(), params);
    }

    private Map<String, Object> stepToMap(PlanStep step) {
        Map<String, Object> m = new HashMap<>(Map.of(
                "type", step.type(),
                "target", step.target() != null ? step.target() : "",
                "explanation", step.explanation() != null ? step.explanation() : ""
        ));
        if (step.parameters() != null && !step.parameters().isEmpty()) {
            m.put("parameters", new HashMap<>(step.parameters()));
        }
        return m;
    }

    private PlanStep mapToStep(Map<String, Object> m) {
        String type = (String) m.get("type");
        String target = (String) m.get("target");
        String explanation = (String) m.get("explanation");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) m.get("parameters");
        return new PlanStep(type != null ? type : "explain", target, explanation, parameters != null ? parameters : Map.of());
    }

    private static PlanEntity.PlanStatus convertStatus(Plan.PlanStatus s) {
        return s == null ? PlanEntity.PlanStatus.CREATED : switch (s) {
            case CREATED -> PlanEntity.PlanStatus.CREATED;
            case EXECUTING -> PlanEntity.PlanStatus.EXECUTING;
            case COMPLETED -> PlanEntity.PlanStatus.COMPLETED;
            case FAILED -> PlanEntity.PlanStatus.FAILED;
            case CANCELLED -> PlanEntity.PlanStatus.CANCELLED;
        };
    }

    private static Plan.PlanStatus convertStatus(PlanEntity.PlanStatus s) {
        return s == null ? Plan.PlanStatus.CREATED : switch (s) {
            case CREATED -> Plan.PlanStatus.CREATED;
            case EXECUTING -> Plan.PlanStatus.EXECUTING;
            case COMPLETED -> Plan.PlanStatus.COMPLETED;
            case FAILED -> Plan.PlanStatus.FAILED;
            case CANCELLED -> Plan.PlanStatus.CANCELLED;
        };
    }

    private static Plan.PlanStatus parseStatus(String s) {
        if (s == null) return Plan.PlanStatus.CREATED;
        return switch (s) {
            case "CREATED" -> Plan.PlanStatus.CREATED;
            case "EXECUTING" -> Plan.PlanStatus.EXECUTING;
            case "COMPLETED" -> Plan.PlanStatus.COMPLETED;
            case "FAILED" -> Plan.PlanStatus.FAILED;
            case "CANCELLED" -> Plan.PlanStatus.CANCELLED;
            default -> Plan.PlanStatus.CREATED;
        };
    }
}
