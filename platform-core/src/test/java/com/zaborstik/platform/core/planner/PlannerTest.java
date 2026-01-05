package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.resolver.InMemoryResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    private InMemoryResolver resolver;
    private Planner planner;

    @BeforeEach
    void setUp() {
        resolver = new InMemoryResolver();
        planner = new Planner(resolver);
    }

    @Test
    void shouldCreateLinearPlan() {
        // Регистрируем метаданные
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action(
            "order_egrn_extract",
            "Заказать выписку из ЕГРН",
            "Заказывает выписку из ЕГРН для здания",
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
        Plan plan = planner.createPlan(request);

        // Проверяем результат
        assertNotNull(plan);
        assertEquals("Building", plan.getEntityTypeId());
        assertEquals("93939", plan.getEntityId());
        assertEquals("order_egrn_extract", plan.getActionId());
        assertEquals(Plan.PlanStatus.CREATED, plan.getStatus());

        // Проверяем шаги плана
        var steps = plan.getSteps();
        assertFalse(steps.isEmpty());
        assertEquals(5, steps.size());

        // Проверяем структуру шагов
        PlanStep step1 = steps.get(0);
        assertEquals("open_page", step1.getType());
        assertTrue(step1.getTarget().contains("/buildings/93939"));

        PlanStep step2 = steps.get(1);
        assertEquals("explain", step2.getType());
        assertNotNull(step2.getExplanation());

        PlanStep step3 = steps.get(2);
        assertEquals("hover", step3.getType());
        assertTrue(step3.getTarget().contains("order_egrn_extract"));

        PlanStep step4 = steps.get(3);
        assertEquals("click", step4.getType());
        assertTrue(step4.getTarget().contains("order_egrn_extract"));

        PlanStep step5 = steps.get(4);
        assertEquals("wait", step5.getType());
        assertEquals("result", step5.getTarget());
    }

    @Test
    void shouldThrowExceptionWhenEntityTypeNotFound() {
        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("NonExistent", "123", "action", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            planner.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenActionNotFound() {
        EntityType entityType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(entityType);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "non_existent", Map.of());

        assertThrows(IllegalArgumentException.class, () -> {
            planner.createPlan(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenActionNotApplicableToEntityType() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action(
            "action",
            "Action",
            "Desc",
            Set.of("Contract"), // Действие применимо только к Contract
            Map.of()
        );
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            planner.createPlan(request);
        });

        assertTrue(exception.getMessage().contains("not applicable"));
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
            planner.createPlan(request);
        });
    }

    @Test
    void shouldUseActionDescriptionInPlan() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action(
            "action",
            "Action",
            "Кастомное описание действия",
            Set.of("Building"),
            Map.of()
        );
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());
        Plan plan = planner.createPlan(request);

        var steps = plan.getSteps();
        PlanStep explainStep = steps.get(1);
        assertEquals("explain", explainStep.getType());
        assertEquals("Кастомное описание действия", explainStep.getExplanation());
    }

    @Test
    void shouldUseActionNameWhenDescriptionIsNull() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action(
            "action",
            "Название действия",
            null, // Нет описания
            Set.of("Building"),
            Map.of()
        );
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "123", "action", Map.of());
        Plan plan = planner.createPlan(request);

        var steps = plan.getSteps();
        PlanStep explainStep = steps.get(1);
        assertEquals("explain", explainStep.getType());
        assertTrue(explainStep.getExplanation().contains("Название действия"));
    }

    @Test
    void shouldBuildCorrectPageUrl() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        resolver.registerEntityType(buildingType);

        Action action = new Action("action", "Action", "Desc", Set.of("Building"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        ExecutionRequest request = new ExecutionRequest("Building", "93939", "action", Map.of());
        Plan plan = planner.createPlan(request);

        var steps = plan.getSteps();
        PlanStep openPageStep = steps.get(0);
        assertEquals("open_page", openPageStep.getType());
        assertEquals("/buildings/93939", openPageStep.getTarget());
    }

    @Test
    void shouldHandleMultipleEntityTypes() {
        EntityType buildingType = new EntityType("Building", "Здание", Map.of());
        EntityType contractType = new EntityType("Contract", "Договор", Map.of());
        resolver.registerEntityType(buildingType);
        resolver.registerEntityType(contractType);

        Action action = new Action("action", "Action", "Desc", Set.of("Building", "Contract"), Map.of());
        resolver.registerAction(action);

        UIBinding uiBinding = new UIBinding("action", "selector", UIBinding.SelectorType.CSS, Map.of());
        resolver.registerUIBinding(uiBinding);

        // План для Building
        ExecutionRequest request1 = new ExecutionRequest("Building", "123", "action", Map.of());
        Plan plan1 = planner.createPlan(request1);
        assertEquals("Building", plan1.getEntityTypeId());

        // План для Contract
        ExecutionRequest request2 = new ExecutionRequest("Contract", "456", "action", Map.of());
        Plan plan2 = planner.createPlan(request2);
        assertEquals("Contract", plan2.getEntityTypeId());
    }
}

