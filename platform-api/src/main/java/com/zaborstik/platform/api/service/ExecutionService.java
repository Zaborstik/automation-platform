package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.PlanRepository;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Сервис выполнения. Схема по newdatabase.drawio: plan/plan_step в system.
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

    @Transactional
    public EntityDTO createPlan(EntityDTO request) {
        if (!EntityDTO.TABLE_EXECUTION_REQUEST.equals(request.getTableName())) {
            throw new IllegalArgumentException("Expected tableName=" + EntityDTO.TABLE_EXECUTION_REQUEST);
        }
        ExecutionRequest execRequest = toExecutionRequest(request);
        Plan plan = executionEngine.createPlan(execRequest);

        PlanEntity entity = planMapper.toEntity(plan);
        planRepository.save(entity);
        return planMapper.toEntityDTO(plan);
    }

    @Transactional(readOnly = true)
    public Optional<EntityDTO> getPlan(String planId) {
        return planRepository.findById(planId)
                .map(planMapper::toDomain)
                .map(planMapper::toEntityDTO);
    }

    private ExecutionRequest toExecutionRequest(EntityDTO dto) {
        Map<String, Object> data = dto.getData();
        String entityType = (String) data.get("entity");
        String entityId = (String) data.get("entityId");
        String action = (String) data.get("action");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) data.get("parameters");
        if (entityType == null || entityId == null || action == null) {
            throw new IllegalArgumentException("data must contain entity, entityId, action");
        }
        return new ExecutionRequest(
                entityType,
                entityId,
                action,
                parameters != null ? parameters : Map.of()
        );
    }
}
