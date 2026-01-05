package com.zaborstik.platform.executor;

import com.zaborstik.platform.agent.dto.StepExecutionResult;
import com.zaborstik.platform.agent.service.AgentService;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanExecutorTest {

    @Mock
    private AgentService agentService;

    private PlanExecutor executor;
    private Plan testPlan;

    @BeforeEach
    void setUp() {
        executor = new PlanExecutor(agentService);

        List<PlanStep> steps = List.of(
            PlanStep.openPage("/buildings/93939", "Открываю карточку здания"),
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
    void shouldExecutePlanSuccessfully() {
        // Given
        List<StepExecutionResult> successResults = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null),
            StepExecutionResult.success("hover", "action(order_egrn_extract)", "Element hovered", 150L, null),
            StepExecutionResult.success("click", "action(order_egrn_extract)", "Element clicked", 200L, null),
            StepExecutionResult.success("wait", "result", "Condition met", 3000L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(successResults);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertNotNull(result);
        assertEquals(testPlan.getId(), result.getPlanId());
        assertTrue(result.isSuccess());
        assertEquals(5, result.getLogEntries().size());
        assertEquals(5, result.getStepResults().size());

        // Проверяем, что все шаги успешны
        assertTrue(result.getStepResults().stream().allMatch(StepExecutionResult::isSuccess));
    }

    @Test
    void shouldHandlePlanWithFailure() {
        // Given
        List<StepExecutionResult> resultsWithFailure = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null),
            StepExecutionResult.failure("hover", "action(order_egrn_extract)", "Element not found", 100L),
            StepExecutionResult.success("click", "action(order_egrn_extract)", "Element clicked", 200L, null),
            StepExecutionResult.success("wait", "result", "Condition met", 3000L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(resultsWithFailure);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess()); // Общий статус должен быть failure
        assertEquals(5, result.getLogEntries().size());

        // Проверяем, что третий шаг неудачен
        ExecutionLogEntry failedEntry = result.getLogEntries().get(2);
        assertFalse(failedEntry.getResult().isSuccess());
        assertEquals("Element not found", failedEntry.getResult().getError());
    }

    @Test
    void shouldHandlePlanWithAllFailures() {
        // Given
        List<StepExecutionResult> allFailures = List.of(
            StepExecutionResult.failure("open_page", "/buildings/93939", "Page not found", 100L),
            StepExecutionResult.failure("explain", null, "Failed to log", 5L)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(allFailures);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(5, result.getLogEntries().size()); // Все шаги должны быть в логе

        // Первые два шага должны быть неудачными
        assertFalse(result.getLogEntries().get(0).getResult().isSuccess());
        assertFalse(result.getLogEntries().get(1).getResult().isSuccess());
    }

    @Test
    void shouldHandleWhenAgentReturnsLessResultsThanSteps() {
        // Given
        // План имеет 5 шагов, но агент вернул только 3 результата
        List<StepExecutionResult> partialResults = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null),
            StepExecutionResult.success("hover", "action(order_egrn_extract)", "Element hovered", 150L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(partialResults);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getLogEntries().size()); // Все шаги должны быть в логе

        // Первые 3 шага должны быть успешными
        assertTrue(result.getLogEntries().get(0).getResult().isSuccess());
        assertTrue(result.getLogEntries().get(1).getResult().isSuccess());
        assertTrue(result.getLogEntries().get(2).getResult().isSuccess());

        // Последние 2 шага должны быть синтетическими failure
        ExecutionLogEntry syntheticFailure1 = result.getLogEntries().get(3);
        assertFalse(syntheticFailure1.getResult().isSuccess());
        assertTrue(syntheticFailure1.getResult().getError().contains("Step was not executed by agent"));

        ExecutionLogEntry syntheticFailure2 = result.getLogEntries().get(4);
        assertFalse(syntheticFailure2.getResult().isSuccess());
        assertTrue(syntheticFailure2.getResult().getError().contains("Step was not executed by agent"));

        // Общий статус должен быть failure
        assertFalse(result.isSuccess());
    }

    @Test
    void shouldHandleWhenAgentReturnsMoreResultsThanSteps() {
        // Given
        // План имеет 5 шагов, но агент вернул 7 результатов (необычный случай)
        List<StepExecutionResult> extraResults = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null),
            StepExecutionResult.success("hover", "action(order_egrn_extract)", "Element hovered", 150L, null),
            StepExecutionResult.success("click", "action(order_egrn_extract)", "Element clicked", 200L, null),
            StepExecutionResult.success("wait", "result", "Condition met", 3000L, null),
            StepExecutionResult.success("extra1", "extra", "Extra step 1", 100L, null),
            StepExecutionResult.success("extra2", "extra", "Extra step 2", 100L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(extraResults);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getLogEntries().size()); // Только шаги из плана должны быть в логе
        assertTrue(result.isSuccess()); // Все шаги плана успешны
    }

    @Test
    void shouldHandleEmptyPlan() {
        // Given
        Plan emptyPlan = new Plan("Building", "123", "action", List.of());
        when(agentService.executePlan(any(Plan.class))).thenReturn(List.of());

        // When
        PlanExecutionResult result = executor.execute(emptyPlan);

        // Then
        assertNotNull(result);
        assertEquals(emptyPlan.getId(), result.getPlanId());
        assertTrue(result.isSuccess()); // Пустой план считается успешным
        assertTrue(result.getLogEntries().isEmpty());
        assertTrue(result.getStepResults().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenPlanIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            executor.execute(null);
        });
    }

    @Test
    void shouldThrowExceptionWhenAgentServiceIsNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            new PlanExecutor(null);
        });
    }

    @Test
    void shouldCreateExecutionLogWithCorrectStepIndices() {
        // Given
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null),
            StepExecutionResult.success("hover", "action(order_egrn_extract)", "Element hovered", 150L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(results);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        assertEquals(5, result.getLogEntries().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, result.getLogEntries().get(i).getStepIndex());
        }
    }

    @Test
    void shouldPreserveStepInformationInLogEntries() {
        // Given
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null),
            StepExecutionResult.success("explain", null, "Message logged", 5L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(results);

        // When
        PlanExecutionResult result = executor.execute(testPlan);

        // Then
        ExecutionLogEntry entry0 = result.getLogEntries().get(0);
        assertEquals("open_page", entry0.getStep().getType());
        assertEquals("/buildings/93939", entry0.getStep().getTarget());
        assertEquals("Открываю карточку здания", entry0.getStep().getExplanation());

        ExecutionLogEntry entry1 = result.getLogEntries().get(1);
        assertEquals("explain", entry1.getStep().getType());
        assertEquals("Выполняю действие", entry1.getStep().getExplanation());
    }

    @Test
    void shouldSetCorrectTimestamps() {
        // Given
        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("open_page", "/buildings/93939", "Page opened", 1200L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(results);

        Plan planWithOneStep = new Plan(
            "Building",
            "123",
            "action",
            List.of(PlanStep.openPage("/test", "Test"))
        );

        // When
        PlanExecutionResult result = executor.execute(planWithOneStep);

        // Then
        assertNotNull(result.getStartedAt());
        assertNotNull(result.getFinishedAt());
        assertTrue(result.getFinishedAt().isAfter(result.getStartedAt()) ||
                   result.getFinishedAt().equals(result.getStartedAt()));

        // Все записи в логе должны иметь временные метки
        result.getLogEntries().forEach(entry -> {
            assertNotNull(entry.getLoggedAt());
        });
    }

    @Test
    void shouldHandlePlanWithSingleStep() {
        // Given
        Plan singleStepPlan = new Plan(
            "Building",
            "123",
            "action",
            List.of(PlanStep.explain("Single step"))
        );

        List<StepExecutionResult> results = List.of(
            StepExecutionResult.success("explain", null, "OK", 10L, null)
        );

        when(agentService.executePlan(any(Plan.class))).thenReturn(results);

        // When
        PlanExecutionResult result = executor.execute(singleStepPlan);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getLogEntries().size());
        assertTrue(result.isSuccess());
    }
}

