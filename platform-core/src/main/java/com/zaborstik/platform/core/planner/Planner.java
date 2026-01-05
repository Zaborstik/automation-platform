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
     */
    public Plan createPlan(ExecutionRequest request) {
        // 1. Находим EntityType
        EntityType entityType = resolver.findEntityType(request.getEntityType())
            .orElseThrow(() -> new IllegalArgumentException(
                "EntityType not found: " + request.getEntityType()));

        // 2. Находим Action
        Action action = resolver.findAction(request.getAction())
            .orElseThrow(() -> new IllegalArgumentException(
                "Action not found: " + request.getAction()));

        // 3. Проверяем применимость
        if (!action.isApplicableTo(entityType.getId())) {
            throw new IllegalArgumentException(
                "Action '" + action.getId() + "' is not applicable to entity type '" + 
                entityType.getId() + "'");
        }

        // 4. Находим UIBinding
        UIBinding uiBinding = resolver.findUIBinding(action.getId())
            .orElseThrow(() -> new IllegalArgumentException(
                "UIBinding not found for action: " + action.getId()));

        // 5. Строим план
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
     */
    private List<PlanStep> buildLinearPlan(EntityType entityType, Action action, 
                                           UIBinding uiBinding, ExecutionRequest request) {
        List<PlanStep> steps = new ArrayList<>();

        // 1. Открываем страницу сущности
        String pageUrl = buildEntityPageUrl(entityType, request.getEntityId());
        steps.add(PlanStep.openPage(pageUrl, 
            "Открываю карточку " + entityType.getName() + " #" + request.getEntityId()));

        // 2. Объясняем действие
        String explanation = action.getDescription() != null 
            ? action.getDescription() 
            : "Выполняю действие: " + action.getName();
        steps.add(PlanStep.explain(explanation));

        // 3. Наводим на элемент (для визуализации)
        steps.add(PlanStep.hover(action.getId(), 
            "Навожу курсор на элемент действия '" + action.getName() + "'"));

        // 4. Кликаем
        steps.add(PlanStep.click(action.getId(), 
            "Выполняю действие '" + action.getName() + "'"));

        // 5. Ждем результата
        steps.add(PlanStep.wait("result", 
            "Ожидаю завершения действия '" + action.getName() + "'"));

        return steps;
    }

    /**
     * Строит URL страницы сущности.
     * В будущем это может быть конфигурируемо через метаданные EntityType.
     */
    private String buildEntityPageUrl(EntityType entityType, String entityId) {
        // Простая логика: /{entityType}/{entityId}
        // В реальности может быть сложнее и конфигурироваться через метаданные
        String entityPath = entityType.getId().toLowerCase() + "s"; // pluralize
        return "/" + entityPath + "/" + entityId;
    }
}

