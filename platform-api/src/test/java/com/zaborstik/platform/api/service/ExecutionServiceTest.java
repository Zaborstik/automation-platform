package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.entity.PlanEntity;
import com.zaborstik.platform.api.mapper.PlanMapper;
import com.zaborstik.platform.api.repository.PlanRepository;
import com.zaborstik.platform.core.ExecutionEngine;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private ExecutionEngine executionEngine;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanMapper planMapper;

    @InjectMocks
    private ExecutionService executionService;

    private Plan testPlan;

    @BeforeEach
    void setUp() {
        List<PlanStep> steps = List.of(
                PlanStep.openPage("/buildings/93939", "Открываю карточку"),
                PlanStep.explain("Выполняю действие"),
                PlanStep.hover("order_egrn_extract", "Навожу курсор"),
                PlanStep.click("order_egrn_extract", "Кликаю"),
                PlanStep.wait("result", "Жду результат")
        );
        testPlan = new Plan("Building", "93939", "order_egrn_extract", steps);
    }

    @Test
    void shouldCreatePlanSuccessfully() {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null,
                Map.of("entity", "Building", "entityId", "93939", "action", "order_egrn_extract", "parameters", Map.of()));

        EntityDTO planDto = new EntityDTO(EntityDTO.TABLE_PLANS, testPlan.id(),
                Map.of("entityTypeId", "Building", "entityId", "93939", "actionId", "order_egrn_extract", "status", "CREATED", "steps", List.of()));

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(testPlan);
        when(planMapper.toEntity(any(Plan.class))).thenReturn(new PlanEntity());
        when(planMapper.toEntityDTO(any(Plan.class))).thenReturn(planDto);
        when(planRepository.save(any(PlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EntityDTO result = executionService.createPlan(request);

        assertNotNull(result);
        assertEquals(EntityDTO.TABLE_PLANS, result.getTableName());
        assertEquals(testPlan.id(), result.getId());
        assertEquals("Building", result.get("entityTypeId"));
        assertEquals("93939", result.get("entityId"));
        assertEquals("order_egrn_extract", result.get("actionId"));
        assertEquals("CREATED", result.get("status"));
        verify(planRepository, times(1)).save(any(PlanEntity.class));
        verify(planMapper, times(1)).toEntityDTO(testPlan);
    }

    @Test
    void shouldThrowWhenTableNameNotExecutionRequest() {
        EntityDTO request = new EntityDTO("other", null, Map.of("entity", "B", "entityId", "1", "action", "a"));
        assertThrows(IllegalArgumentException.class, () -> executionService.createPlan(request));
    }

    @Test
    void shouldThrowWhenDataMissingRequiredFields() {
        EntityDTO request = new EntityDTO(EntityDTO.TABLE_EXECUTION_REQUEST, null, Map.of("entity", "Building"));
        assertThrows(IllegalArgumentException.class, () -> executionService.createPlan(request));
    }

    @Test
    void shouldGetPlanFromDatabase() {
        String planId = "test-plan-id";
        PlanEntity planEntity = new PlanEntity();
        planEntity.setShortname(planId);
        EntityDTO planDto = new EntityDTO(EntityDTO.TABLE_PLANS, planId,
                Map.of("entityTypeId", "Building", "entityId", "93939", "actionId", "order_egrn_extract", "status", "CREATED", "steps", List.of()));

        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(planMapper.toDomain(planEntity)).thenReturn(testPlan);
        when(planMapper.toEntityDTO(testPlan)).thenReturn(planDto);

        Optional<EntityDTO> result = executionService.getPlan(planId);

        assertTrue(result.isPresent());
        assertEquals(EntityDTO.TABLE_PLANS, result.get().getTableName());
        assertEquals("Building", result.get().get("entityTypeId"));
        verify(planRepository, times(1)).findById(planId);
    }

    @Test
    void shouldReturnEmptyWhenPlanNotFound() {
        when(planRepository.findById("non-existent")).thenReturn(Optional.empty());
        Optional<EntityDTO> result = executionService.getPlan("non-existent");
        assertFalse(result.isPresent());
        verify(planMapper, never()).toDomain(any());
    }
}
