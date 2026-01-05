package org.example.api.service;

import org.example.api.dto.ExecutionRequestDTO;
import org.example.api.dto.PlanDTO;
import org.example.api.dto.PlanStepDTO;
import org.example.core.ExecutionEngine;
import org.example.core.execution.ExecutionRequest;
import org.example.core.plan.Plan;
import org.example.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private ExecutionEngine executionEngine;

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

        // When
        PlanDTO result = executionService.createPlan(requestDTO);

        // Then
        assertNotNull(result);
        assertEquals("Building", result.getEntityTypeId());
        assertEquals("93939", result.getEntityId());
        assertEquals("order_egrn_extract", result.getActionId());
        assertEquals("CREATED", result.getStatus());
        assertEquals(5, result.getSteps().size());
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
}

