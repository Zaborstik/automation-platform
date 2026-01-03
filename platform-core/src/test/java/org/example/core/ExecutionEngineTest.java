package org.example.core;

import org.example.core.domain.Action;
import org.example.core.domain.EntityType;
import org.example.core.domain.UIBinding;
import org.example.core.execution.ExecutionRequest;
import org.example.core.plan.Plan;
import org.example.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionEngineTest {

    private InMemoryResolver resolver;
    private ExecutionEngine engine;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        engine = new ExecutionEngine(resolver);
    }

    @Test
    void shouldCreatePlanSuccessfully() {
        // Настраиваем метаданные
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Описание действия",
            Set.of("Building"),
            Map.of()
        );
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding(
            "order_egrn_extract",
            "[data-action='order_egrn_extract']",
            UIBinding.SelectorType.CSS,
            Map.of()
        );
        resolver.registerUIBinding(uiBinding);

        // Создаем запрос
        ExecutionRequest request = new ExecutionRequest(
            "Building",
            "93939",
            "order_egrn_extract",
            Map.of()
        );

        // Создаем план
        Plan plan = engine.createPlan(request);

        // Проверяем результат
        assertNotNull(plan);
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertFalse(plan.getSteps().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenEntityTypeNotFound() {
        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("NonExistent", "123", "action", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenActionNotFound() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "non_existent", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenActionNotApplicable() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action("action", "Action", "Desc", Set.of("Contract"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenUIBindingNotFound() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        // Не регистрируем UIBinding

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            engine.createPlan(request);
        });
    }

    @Test
    void shouldCreateEngineWithResolver() {
        ExecutionEngine engine1 = new ExecutionEngine(resolver);
        assertNotNull(engine1);
    }

    @Test
    void shouldCreateEngineWithPlanner() {
        org.example.core.planner.Planner planner = new org.example.core.planner.Planner(resolver);
        ExecutionEngine engine1 = new ExecutionEngine(planner);
        assertNotNull(engine1);
    }

    @Test
    void shouldHandleMultipleRequests() {
        // Настраиваем метаданные
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        // Первый запрос
        ExecutionRequest request1 = new ExecutionRequest("Building", "123", "action", Map.of());
        Plan plan1 = engine.createPlan(request1);
        assertNotNull(plan1);
        assertEquals("123", plan1.getEntityId());

        // Второй запрос
        ExecutionRequest request2 = new ExecutionRequest("Building", "456", "action", Map.of());
        Plan plan2 = engine.createPlan(request2);
        assertNotNull(plan2);
        assertEquals("456", plan2.getEntityId());

        // Планы должны быть разными
        assertNotEquals(plan1.getId(), plan2.getId());
    }

    @Test
    void shouldCreatePlanWithCorrectSteps() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());
        Plan plan = engine.createPlan(request);

        var steps = plan.getSteps();
        assertEquals(5, steps.size());
        assertEquals("open_page", steps.get(0).getType());
        assertEquals("explain", steps.get(1).getType());
        assertEquals("hover", steps.get(2).getType());
        assertEquals("click", steps.get(3).getType());
        assertEquals("wait", steps.get(4).getType());
    }
}

