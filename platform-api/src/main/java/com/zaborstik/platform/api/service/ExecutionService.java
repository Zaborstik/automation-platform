package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.EntityRepository;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Сервис выполнения. Работает только с EntityDTO и единой таблицей entities.
 */
@Service
public class ExecutionService {
    private final ExecutionEngine executionEngine;
    private final EntityRepository entityRepository;
    private final PlanMapper planMapper;

    public ExecutionService(ExecutionEngine executionEngine,
                            EntityRepository entityRepository,
                            PlanMapper planMapper) {
        this.executionEngine = executionEngine;
        this.entityRepository = entityRepository;
        this.planMapper = planMapper;
    }

    @Transactional
    public EntityDTO createPlan(EntityDTO request) {
        if (!EntityDTO.TABLE_EXECUTION_REQUEST.equals(request.getTableName())) {
            throw new IllegalArgumentException("Expected tableName=" + EntityDTO.TABLE_EXECUTION_REQUEST);
        }
        ExecutionRequest execRequest = toExecutionRequest(request);
        Plan plan = executionEngine.createPlan(execRequest);

        EntityDTO dto = planMapper.toEntityDTO(plan);
        return entityRepository.save(dto);
    }

    @Transactional(readOnly = true)
    public Optional<EntityDTO> getPlan(String planId) {
        return entityRepository.findByTableNameAndId(EntityDTO.TABLE_PLANS, planId);
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
