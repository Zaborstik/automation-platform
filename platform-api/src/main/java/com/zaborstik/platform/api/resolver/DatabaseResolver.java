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
 * Resolver по схеме newdatabase.drawio: zbrtstk (entity_type), system (action, ui_binding).
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
        return entityTypeRepository.findById(entityTypeId).map(this::toEntityType);
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return actionRepository.findById(actionId).map(this::toAction);
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return uiBindingRepository.findById(actionId).map(this::toUIBinding);
    }

    private EntityType toEntityType(EntityTypeEntity e) {
        Map<String, Object> meta = e.getMetadata().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> (Object) v.getValue()));
        return new EntityType(e.getId(), e.getName(), meta);
    }

    private Action toAction(ActionEntity e) {
        Set<String> applicable = e.getApplicableEntityTypes();
        Map<String, Object> meta = e.getMetadata().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> (Object) v.getValue()));
        return new Action(e.getId(), e.getName(), e.getDescription() != null ? e.getDescription() : "",
                applicable, meta);
    }

    private UIBinding toUIBinding(UIBindingEntity e) {
        UIBinding.SelectorType st = switch (e.getSelectorType().name()) {
            case "CSS" -> UIBinding.SelectorType.CSS;
            case "XPATH" -> UIBinding.SelectorType.XPATH;
            case "TEXT" -> UIBinding.SelectorType.TEXT;
            case "ACTION_ID" -> UIBinding.SelectorType.ACTION_ID;
            default -> UIBinding.SelectorType.CSS;
        };
        Map<String, Object> meta = e.getMetadata().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> (Object) v.getValue()));
        return new UIBinding(e.getActionId(), e.getSelector(), st, meta);
    }
}
