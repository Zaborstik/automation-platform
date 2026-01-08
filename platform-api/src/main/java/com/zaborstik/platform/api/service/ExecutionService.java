package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
import com.zaborstik.platform.api.dto.PlanStepDTO;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.PlanRepository;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с выполнением действий.
 * Преобразует DTO в доменные объекты и обратно.
 * Сохраняет планы в БД.
 * 
 * Service for working with action execution.
 * Converts DTOs to domain objects and back.
 * Saves plans to database.
 */
@Service
public class ExecutionService {
    private final ExecutionEngine executionEngine;
    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    public ExecutionService(ExecutionEngine executionEngine, 
                          PlanRepository planRepository,
                          PlanMapper planMapper) {
        this.executionEngine = executionEngine;
        this.planRepository = planRepository;
        this.planMapper = planMapper;
    }

    /**
     * Создает план выполнения для запроса и сохраняет его в БД.
     *
     * @param requestDTO DTO запроса
     * @return DTO плана выполнения
     */
    @Transactional
    public PlanDTO createPlan(ExecutionRequestDTO requestDTO) {
        ExecutionRequest request = toExecutionRequest(requestDTO);
        Plan plan = executionEngine.createPlan(request);
        
        // Сохраняем план в БД
        PlanEntity planEntity = planMapper.toEntity(plan);
        planRepository.save(planEntity);
        
        return toPlanDTO(plan);
    }

    /**
     * Получает план по идентификатору из БД.
     *
     * @param planId идентификатор плана
     * @return DTO плана выполнения или пустой Optional, если план не найден
     */
    @Transactional(readOnly = true)
    public Optional<PlanDTO> getPlan(String planId) {
        return planRepository.findById(planId)
            .map(planMapper::toDomain)
            .map(this::toPlanDTO);
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

