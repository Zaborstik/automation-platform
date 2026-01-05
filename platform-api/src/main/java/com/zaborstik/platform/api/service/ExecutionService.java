package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
import com.zaborstik.platform.api.dto.PlanStepDTO;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для работы с выполнением действий.
 * Преобразует DTO в доменные объекты и обратно.
 */
@Service
public class ExecutionService {
    private final ExecutionEngine executionEngine;

    public ExecutionService(ExecutionEngine executionEngine) {
        this.executionEngine = executionEngine;
    }

    /**
     * Создает план выполнения для запроса.
     *
     * @param requestDTO DTO запроса
     * @return DTO плана выполнения
     */
    public PlanDTO createPlan(ExecutionRequestDTO requestDTO) {
        ExecutionRequest request = toExecutionRequest(requestDTO);
        Plan plan = executionEngine.createPlan(request);
        return toPlanDTO(plan);
    }

    /**
     * Преобразует ExecutionRequestDTO в ExecutionRequest.
     */
    private ExecutionRequest toExecutionRequest(ExecutionRequestDTO dto) {
        Map<String, Object> parameters = dto.getParameters() != null 
            ? dto.getParameters() 
            : Map.of();
        return new ExecutionRequest(
            dto.getEntityType(),
            dto.getEntityId(),
            dto.getAction(),
            parameters
        );
    }

    /**
     * Преобразует Plan в PlanDTO.
     */
    private PlanDTO toPlanDTO(Plan plan) {
        List<PlanStepDTO> stepDTOs = plan.getSteps().stream()
            .map(this::toPlanStepDTO)
            .collect(Collectors.toList());

        return new PlanDTO(
            plan.getId(),
            plan.getEntityTypeId(),
            plan.getEntityId(),
            plan.getActionId(),
            stepDTOs,
            plan.getStatus().name()
        );
    }

    /**
     * Преобразует PlanStep в PlanStepDTO.
     */
    private PlanStepDTO toPlanStepDTO(PlanStep step) {
        return new PlanStepDTO(
            step.getType(),
            step.getTarget(),
            step.getExplanation(),
            step.getParameters()
        );
    }
}

