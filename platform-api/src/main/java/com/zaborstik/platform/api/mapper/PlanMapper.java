package com.zaborstik.platform.api.mapper;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Маппер Plan ↔ EntityDTO (единая таблица entities).
 */
@Component
public class PlanMapper {

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
        return new PlanStep(
                type != null ? type : "explain",
                target,
                explanation,
                parameters != null ? parameters : Map.of()
        );
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
