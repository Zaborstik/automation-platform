package com.zaborstik.platform.core.resolver;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация Resolver для MVP.
 * В будущем может быть заменена на репозиторий с БД.
 */
public class InMemoryResolver implements Resolver {
    private final Map<String, EntityType> entityTypes = new ConcurrentHashMap<>();
    private final Map<String, Action> actions = new ConcurrentHashMap<>();
    private final Map<String, UIBinding> uiBindings = new ConcurrentHashMap<>();

    public void registerEntityType(EntityType entityType) {
        entityTypes.put(entityType.getId(), entityType);
    }

    public void registerAction(Action action) {
        actions.put(action.getId(), action);
    }

    public void registerUIBinding(UIBinding uiBinding) {
        uiBindings.put(uiBinding.getActionId(), uiBinding);
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return Optional.ofNullable(entityTypes.get(entityTypeId));
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return Optional.ofNullable(actions.get(actionId));
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return Optional.ofNullable(uiBindings.get(actionId));
    }
}

