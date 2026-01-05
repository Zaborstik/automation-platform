package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;

import java.util.Optional;

/**
 * Resolver находит EntityType, Action и ActionUiBinding по запросу.
 * Это ключевой компонент для построения плана выполнения.
 */
public interface Resolver {
    /**
     * Находит EntityType по идентификатору.
     */
    Optional<EntityType> findEntityType(String entityTypeId);

    /**
     * Находит Action по идентификатору.
     */
    Optional<Action> findAction(String actionId);

    /**
     * Находит UIBinding для действия.
     */
    Optional<UIBinding> findUIBinding(String actionId);

    /**
     * Проверяет, применимо ли действие к типу сущности.
     */
    default boolean isActionApplicable(String actionId, String entityTypeId) {
        return findAction(actionId)
            .map(action -> action.isApplicableTo(entityTypeId))
            .orElse(false);
    }
}

