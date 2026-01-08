package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.entity.PlanStepEntity;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между Plan/PlanStep и PlanEntity/PlanStepEntity.
 * 
 * Mapper for converting between Plan/PlanStep and PlanEntity/PlanStepEntity.
 */
@Component
public class PlanMapper {

    /**
     * Преобразует Plan в PlanEntity.
     */
    public PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity(
            plan.id(),
            plan.entityTypeId(),
            plan.entityId(),
            plan.actionId(),
            convertStatus(plan.status())
        );

        // Преобразуем шаги
        List<PlanStepEntity> stepEntities = new java.util.ArrayList<>();
        for (int i = 0; i < plan.steps().size(); i++) {
            stepEntities.add(toStepEntity(entity, i, plan.steps().get(i)));
        }
        entity.setSteps(stepEntities);

        return entity;
    }

    /**
     * Преобразует PlanEntity в Plan.
     */
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

    /**
     * Преобразует PlanStep в PlanStepEntity.
     */
    private PlanStepEntity toStepEntity(PlanEntity planEntity, int index, PlanStep step) {
        Map<String, String> parameters = step.parameters().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() != null ? entry.getValue().toString() : null
            ));

        return new PlanStepEntity(
            planEntity,
            index,
            step.type(),
            step.target(),
            step.explanation(),
            parameters
        );
    }

    /**
     * Преобразует PlanStepEntity в PlanStep.
     */
    private PlanStep toStep(PlanStepEntity entity) {
        Map<String, Object> parameters = entity.getParameters().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new PlanStep(
            entity.getType(),
            entity.getTarget(),
            entity.getExplanation(),
            parameters
        );
    }

    /**
     * Преобразует Plan.PlanStatus в PlanEntity.PlanStatus.
     */
    private PlanEntity.PlanStatus convertStatus(Plan.PlanStatus status) {
        return switch (status) {
            case CREATED -> PlanEntity.PlanStatus.CREATED;
            case EXECUTING -> PlanEntity.PlanStatus.EXECUTING;
            case COMPLETED -> PlanEntity.PlanStatus.COMPLETED;
            case FAILED -> PlanEntity.PlanStatus.FAILED;
            case CANCELLED -> PlanEntity.PlanStatus.CANCELLED;
        };
    }

    /**
     * Преобразует PlanEntity.PlanStatus в Plan.PlanStatus.
     */
    private Plan.PlanStatus convertStatus(PlanEntity.PlanStatus status) {
        return switch (status) {
            case CREATED -> Plan.PlanStatus.CREATED;
            case EXECUTING -> Plan.PlanStatus.EXECUTING;
            case COMPLETED -> Plan.PlanStatus.COMPLETED;
            case FAILED -> Plan.PlanStatus.FAILED;
            case CANCELLED -> Plan.PlanStatus.CANCELLED;
        };
    }
}
