package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;

import java.util.Optional;

/**
 * Resolver находит EntityType, Action и ActionUiBinding по запросу.
 * Это ключевой компонент для построения плана выполнения.
 * 
 * Resolver finds EntityType, Action and ActionUiBinding by request.
 * This is a key component for building execution plan.
 */
public interface Resolver {
    /**
     * Находит EntityType по идентификатору.
     * 
     * Finds EntityType by identifier.
     */
    Optional<EntityType> findEntityType(String entityTypeId);

    /**
     * Находит Action по идентификатору.
     * 
     * Finds Action by identifier.
     */
    Optional<Action> findAction(String actionId);

    /**
     * Находит UIBinding для действия.
     * 
     * Finds UIBinding for action.
     */
    Optional<UIBinding> findUIBinding(String actionId);

    /**
     * Проверяет, применимо ли действие к типу сущности.
     * 
     * Checks if action is applicable to entity type.
     */
    default boolean isActionApplicable(String actionId, String entityTypeId) {
        return findAction(actionId)
            .map(action -> action.isApplicableTo(entityTypeId))
            .orElse(false);
    }
}

