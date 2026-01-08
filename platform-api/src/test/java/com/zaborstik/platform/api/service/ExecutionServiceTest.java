package com.zaborstik.platform.api.service;

import com.zaborstik.platform.api.dto.ExecutionRequestDTO;
import com.zaborstik.platform.api.dto.PlanDTO;
import com.zaborstik.platform.api.dto.PlanStepDTO;
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

        testPlan = new Plan(
            "Building",
            "93939",
            "order_egrn_extract",
            steps
        );
    }

    @Test
    void shouldCreatePlanSuccessfully() {
        // Given
        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(testPlan);
        when(planMapper.toEntity(any(Plan.class))).thenReturn(
            new com.zaborstik.platform.api.entity.PlanEntity(
                testPlan.getId(),
                testPlan.getEntityTypeId(),
                testPlan.getEntityId(),
                testPlan.getActionId(),
                com.zaborstik.platform.api.entity.PlanEntity.PlanStatus.CREATED
            )
        );
        when(planRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        assertNotNull(result);
        assertEquals("Building", result.getEntityTypeId());
        assertEquals("93939", result.getEntityId());
        assertEquals("order_egrn_extract", result.getActionId());
        assertEquals("CREATED", result.getStatus());
        assertEquals(5, result.getSteps().size());
        
        // Проверяем, что план был сохранен в БД
        verify(planRepository, times(1)).save(any());
        verify(planMapper, times(1)).toEntity(testPlan);
    }

    @Test
    void shouldConvertPlanStepsCorrectly() {
        // Given
        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(testPlan);

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        List<PlanStepDTO> steps = result.getSteps();
        assertEquals(5, steps.size());

        PlanStepDTO step1 = steps.get(0);
        assertEquals("open_page", step1.getType());
        assertEquals("/buildings/93939", step1.getTarget());
        assertEquals("Открываю карточку", step1.getExplanation());

        PlanStepDTO step2 = steps.get(1);
        assertEquals("explain", step2.getType());
        assertEquals("Выполняю действие", step2.getExplanation());

        PlanStepDTO step3 = steps.get(2);
        assertEquals("hover", step3.getType());
        assertEquals("action(order_egrn_extract)", step3.getTarget());

        PlanStepDTO step4 = steps.get(3);
        assertEquals("click", step4.getType());
        assertEquals("action(order_egrn_extract)", step4.getTarget());

        PlanStepDTO step5 = steps.get(4);
        assertEquals("wait", step5.getType());
        assertEquals("result", step5.getTarget());
    }

    @Test
    void shouldHandleRequestWithParameters() {
        // Given
        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of("param1", "value1", "param2", 123)
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(testPlan);

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldHandleRequestWithNullParameters() {
        // Given
        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            null
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(testPlan);

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        assertNotNull(result);
    }

    @Test
    void shouldPropagateExceptionFromEngine() {
        // Given
        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class)))
            .thenThrow(new IllegalArgumentException("EntityType not found"));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            executionService.createPlan(requestDTO);
        });
    }

    @Test
    void shouldConvertEmptyStepsList() {
        // Given
        Plan planWithNoSteps = new Plan(
            "Building",
            "93939",
            "order_egrn_extract",
            List.of()
        );

        ExecutionRequestDTO requestDTO = new ExecutionRequestDTO(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        when(executionEngine.createPlan(any(ExecutionRequest.class))).thenReturn(planWithNoSteps);

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        assertNotNull(result);
        assertTrue(result.getSteps().isEmpty());
    }

    @Test
    void shouldGetPlanFromDatabase() {
        // Given
        String planId = "test-plan-id";
        com.zaborstik.platform.api.entity.PlanEntity planEntity = new com.zaborstik.platform.api.entity.PlanEntity(
            planId,
            "Building",
            "93939",
            "order_egrn_extract",
            com.zaborstik.platform.api.entity.PlanEntity.PlanStatus.CREATED
        );

        when(planRepository.findById(planId)).thenReturn(Optional.of(planEntity));
        when(planMapper.toDomain(planEntity)).thenReturn(testPlan);

        // When
        Optional<PlanDTO> result = executionService.getPlan(planId);

        // Then
        assertTrue(result.isPresent());
        PlanDTO planDTO = result.get();
        assertEquals("Building", planDTO.getEntityTypeId());
        assertEquals("93939", planDTO.getEntityId());
        verify(planRepository, times(1)).findById(planId);
        verify(planMapper, times(1)).toDomain(planEntity);
    }

    @Test
    void shouldReturnEmptyWhenPlanNotFound() {
        // Given
        String planId = "non-existent-plan";

        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        // When
        Optional<PlanDTO> result = executionService.getPlan(planId);

        // Then
        assertFalse(result.isPresent());
        verify(planRepository, times(1)).findById(planId);
        verify(planMapper, never()).toDomain(any());
    }
}

