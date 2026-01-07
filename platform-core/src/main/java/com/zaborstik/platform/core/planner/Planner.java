package com.zaborstik.platform.core.planner;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.execution.ExecutionRequest;
import com.zaborstik.platform.core.plan.Plan;
import com.zaborstik.platform.core.plan.PlanStep;
import com.zaborstik.platform.core.resolver.Resolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Planner строит линейные планы выполнения действий.
 * На этапе 1 (MVP) планы линейные и простые.
 * В будущем будет поддерживать preconditions, postconditions, state transitions.
 * 
 * Planner builds linear action execution plans.
 * At stage 1 (MVP) plans are linear and simple.
 * In the future will support preconditions, postconditions, state transitions.
 */
public class Planner {
    private final Resolver resolver;

    public Planner(Resolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Строит план выполнения для запроса.
     * 
     * Алгоритм:
     * 1. Находит EntityType
     * 2. Находит Action
     * 3. Проверяет применимость
     * 4. Находит UIBinding
     * 5. Строит линейный план
     * 
     * Builds execution plan for request.
     * 
     * Algorithm:
     * 1. Finds EntityType
     * 2. Finds Action
     * 3. Checks applicability
     * 4. Finds UIBinding
     * 5. Builds linear plan
     */
    public Plan createPlan(ExecutionRequest request) {
        // 1. Находим EntityType
        // 1. Find EntityType
        EntityType entityType = resolver.findEntityType(request.getEntityType())
            .orElseThrow(() -> new IllegalArgumentException(
                "EntityType not found: " + request.getEntityType()));

        // 2. Находим Action
        // 2. Find Action
        Action action = resolver.findAction(request.getAction())
            .orElseThrow(() -> new IllegalArgumentException(
                "Action not found: " + request.getAction()));

        // 3. Проверяем применимость
        // 3. Check applicability
        if (!action.isApplicableTo(entityType.getId())) {
            throw new IllegalArgumentException(
                "Action '" + action.getId() + "' is not applicable to entity type '" + 
                entityType.getId() + "'");
        }

        // 4. Находим UIBinding
        // 4. Find UIBinding
        UIBinding uiBinding = resolver.findUIBinding(action.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "UIBinding not found for action: " + action.getId()));

        // 5. Строим план
        // 5. Build plan
        List<PlanStep> steps = buildLinearPlan(entityType, action, uiBinding, request);
        
        return new Plan(entityType.getId(), request.getEntityId(), action.getId(), steps);
    }

    /**
     * Строит линейный план выполнения.
     * 
     * Базовая структура:
     * 1. open_page: открываем страницу сущности
     * 2. explain: объясняем действие
     * 3. hover: наводим на элемент
     * 4. click: кликаем
     * 5. wait: ждем результата
     * 
     * Builds linear execution plan.
     * 
     * Basic structure:
     * 1. open_page: open entity page
     * 2. explain: explain action
     * 3. hover: hover over element
     * 4. click: click
     * 5. wait: wait for result
     */
    private List<PlanStep> buildLinearPlan(EntityType entityType, Action action, 
                                           UIBinding uiBinding, ExecutionRequest request) {
        List<PlanStep> steps = new ArrayList<>();

        // 1. Открываем страницу сущности
        // 1. Open entity page
        String pageUrl = buildEntityPageUrl(entityType, request.getEntityId());
        steps.add(PlanStep.openPage(pageUrl, 
            "Открываю карточку " + entityType.getName() + " #" + request.getEntityId()));

        // 2. Объясняем действие
        // 2. Explain action
        String explanation = action.getDescription() != null 
            ? action.getDescription() 
            : "Выполняю действие: " + action.getName();
        steps.add(PlanStep.explain(explanation));

        // 3. Наводим на элемент (для визуализации)
        // 3. Hover over element (for visualization)
        steps.add(PlanStep.hover(action.getId(), 
            "Навожу курсор на элемент действия '" + action.getName() + "'"));

        // 4. Кликаем
        // 4. Click
        steps.add(PlanStep.click(action.getId(), 
            "Выполняю действие '" + action.getName() + "'"));

        // 5. Ждем результата
        // 5. Wait for result
        steps.add(PlanStep.wait("result", 
            "Ожидаю завершения действия '" + action.getName() + "'"));

        return steps;
    }

    /**
     * Строит URL страницы сущности.
     * В будущем это может быть конфигурируемо через метаданные EntityType.
     * 
     * Builds entity page URL.
     * In the future this can be configurable through EntityType metadata.
     */
    private String buildEntityPageUrl(EntityType entityType, String entityId) {
        // Простая логика: /{entityType}/{entityId}
        // В реальности может быть сложнее и конфигурироваться через метаданные
        // Simple logic: /{entityType}/{entityId}
        // In reality can be more complex and configured through metadata
        String entityPath = entityType.getId().toLowerCase() + "s"; // pluralize
        return "/" + entityPath + "/" + entityId;
    }
}

