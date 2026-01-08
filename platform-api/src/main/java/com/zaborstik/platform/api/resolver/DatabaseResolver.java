package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.entity.ActionEntity;
import com.zaborstik.platform.api.entity.EntityTypeEntity;
import com.zaborstik.platform.api.entity.UIBindingEntity;
import com.zaborstik.platform.api.repository.ActionRepository;
import com.zaborstik.platform.api.repository.EntityTypeRepository;
import com.zaborstik.platform.api.repository.UIBindingRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.resolver.Resolver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database реализация Resolver.
 * Использует JPA репозитории для поиска EntityType, Action и UIBinding.
 * 
 * Database implementation of Resolver.
 * Uses JPA repositories to find EntityType, Action and UIBinding.
 */
@Component
public class DatabaseResolver implements Resolver {
    private final EntityTypeRepository entityTypeRepository;
    private final ActionRepository actionRepository;
    private final UIBindingRepository uiBindingRepository;

    public DatabaseResolver(EntityTypeRepository entityTypeRepository,
                           ActionRepository actionRepository,
                           UIBindingRepository uiBindingRepository) {
        this.entityTypeRepository = entityTypeRepository;
        this.actionRepository = actionRepository;
        this.uiBindingRepository = uiBindingRepository;
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return entityTypeRepository.findById(entityTypeId)
            .map(this::toEntityType);
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return actionRepository.findById(actionId)
            .map(this::toAction);
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return uiBindingRepository.findByActionId(actionId)
            .map(this::toUIBinding);
    }

    /**
     * Преобразует EntityTypeEntity в EntityType.
     */
    private EntityType toEntityType(EntityTypeEntity entity) {
        Map<String, Object> metadata = entity.getMetadata().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new EntityType(entity.getId(), entity.getName(), metadata);
    }

    /**
     * Преобразует ActionEntity в Action.
     */
    private Action toAction(ActionEntity entity) {
        Set<String> applicableEntityTypes = entity.getApplicableEntityTypes();
        Map<String, Object> metadata = entity.getMetadata().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new Action(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            applicableEntityTypes,
            metadata
        );
    }

    /**
     * Преобразует UIBindingEntity в UIBinding.
     */
    private UIBinding toUIBinding(UIBindingEntity entity) {
        UIBinding.SelectorType selectorType = convertSelectorType(entity.getSelectorType());
        Map<String, Object> metadata = entity.getMetadata().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new UIBinding(
            entity.getActionId(),
            entity.getSelector(),
            selectorType,
            metadata
        );
    }

    /**
     * Преобразует UIBindingEntity.SelectorType в UIBinding.SelectorType.
     */
    private UIBinding.SelectorType convertSelectorType(UIBindingEntity.SelectorType entityType) {
        return switch (entityType) {
            case CSS -> UIBinding.SelectorType.CSS;
            case XPATH -> UIBinding.SelectorType.XPATH;
            case TEXT -> UIBinding.SelectorType.TEXT;
            case ACTION_ID -> UIBinding.SelectorType.ACTION_ID;
        };
    }
}
