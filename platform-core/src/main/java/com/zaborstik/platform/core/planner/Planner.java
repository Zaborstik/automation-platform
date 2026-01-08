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
 * <p>
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
     * <p>
     * Алгоритм:
     * 1. Находит EntityType
     * 2. Находит Action
     * 3. Проверяет применимость
     * 4. Находит UIBinding
     * 5. Строит линейный план
     * <p>
     * Builds execution plan for request.
     * <p>
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
        EntityType entityType = resolver.findEntityType(request.entityType())
                .orElseThrow(() -> new IllegalArgumentException(
                        "EntityType not found: " + request.entityType()));

        // 2. Находим Action
        // 2. Find Action
        Action action = resolver.findAction(request.action())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action not found: " + request.action()));

        // 3. Проверяем применимость
        // 3. Check applicability
        if (!action.isApplicableTo(entityType.id())) {
            throw new IllegalArgumentException(
                    "Action '" + action.id() + "' is not applicable to entity type '" +
                            entityType.id() + "'");
        }

        // 4. Находим UIBinding
        // 4. Find UIBinding
        UIBinding uiBinding = resolver.findUIBinding(action.id())
                .orElseThrow(() -> new IllegalArgumentException(
                        "UIBinding not found for action: " + action.id()));

        // 5. Строим план
        // 5. Build plan
        List<PlanStep> steps = buildLinearPlan(entityType, action, uiBinding, request);

        return new Plan(entityType.id(), request.entityId(), action.id(), steps);
    }

    /**
     * Строит линейный план выполнения.
     * <p>
     * Базовая структура:
     * 1. open_page: открываем страницу сущности
     * 2. explain: объясняем действие
     * 3. hover: наводим на элемент
     * 4. click: кликаем
     * 5. wait: ждем результата
     * <p>
     * Builds linear execution plan.
     * <p>
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
        String pageUrl = buildEntityPageUrl(entityType, request.entityId());
        steps.add(PlanStep.openPage(pageUrl,
                "Открываю карточку " + entityType.name() + " #" + request.entityId()));

        // 2. Объясняем действие
        // 2. Explain action
        String explanation = action.description() != null
                ? action.description()
                : "Выполняю действие: " + action.name();
        steps.add(PlanStep.explain(explanation));

        // 3. Наводим на элемент (для визуализации)
        // 3. Hover over element (for visualization)
        steps.add(PlanStep.hover(action.id(),
                "Навожу курсор на элемент действия '" + action.name() + "'"));

        // 4. Кликаем
        // 4. Click
        steps.add(PlanStep.click(action.id(),
                "Выполняю действие '" + action.name() + "'"));

        // 5. Ждем результата
        // 5. Wait for result
        steps.add(PlanStep.wait("result",
                "Ожидаю завершения действия '" + action.name() + "'"));

        return steps;
    }

    /**
     * Строит URL страницы сущности.
     * В будущем это может быть конфигурируемо через метаданные EntityType.
     * <p>
     * Builds entity page URL.
     * In the future this can be configurable through EntityType metadata.
     */
    private String buildEntityPageUrl(EntityType entityType, String entityId) {
        // Простая логика: /{entityType}/{entityId}
        // В реальности может быть сложнее и конфигурироваться через метаданные
        // Simple logic: /{entityType}/{entityId}
        // In reality can be more complex and configured through metadata
        String entityPath = entityType.id().toLowerCase() + "s"; // pluralize
        return "/" + entityPath + "/" + entityId;
    }
}

