package com.zaborstik.platform.api.resolver;

import com.zaborstik.platform.api.dto.EntityDTO;
import com.zaborstik.platform.api.repository.EntityRepository;
import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.domain.EntityType;
import com.zaborstik.platform.core.domain.UIBinding;
import com.zaborstik.platform.core.resolver.Resolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolver через единую таблицу entities (EntityDTO).
 */
@Component
public class DatabaseResolver implements Resolver {
    private final EntityRepository entityRepository;

    public DatabaseResolver(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @Override
    public Optional<EntityType> findEntityType(String entityTypeId) {
        return entityRepository.findByTableNameAndId(EntityDTO.TABLE_ENTITY_TYPES, entityTypeId)
                .map(this::toEntityType);
    }

    @Override
    public Optional<Action> findAction(String actionId) {
        return entityRepository.findByTableNameAndId(EntityDTO.TABLE_ACTIONS, actionId)
                .map(this::toAction);
    }

    @Override
    public Optional<UIBinding> findUIBinding(String actionId) {
        return entityRepository.findByTableNameAndId(EntityDTO.TABLE_UI_BINDINGS, actionId)
                .map(this::toUIBinding);
    }

    private EntityType toEntityType(EntityDTO dto) {
        String name = dto.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) dto.getData().get("metadata");
        if (metadata == null) metadata = Map.of();
        Map<String, Object> meta = metadata.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) String.valueOf(e.getValue())));
        return new EntityType(dto.getId(), name != null ? name : dto.getId(), meta);
    }

    private Action toAction(EntityDTO dto) {
        String name = dto.get("name");
        String description = dto.get("description");
        @SuppressWarnings("unchecked")
        Set<String> applicableEntityTypes = dto.get("applicableEntityTypes") != null
                ? new java.util.HashSet<>((Collection<String>) dto.get("applicableEntityTypes"))
                : Set.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) dto.getData().get("metadata");
        if (metadata == null) metadata = Map.of();
        Map<String, Object> meta = metadata.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) String.valueOf(e.getValue())));
        return new Action(
                dto.getId(),
                name != null ? name : dto.getId(),
                description != null ? description : "",
                applicableEntityTypes,
                meta
        );
    }

    private UIBinding toUIBinding(EntityDTO dto) {
        String selector = dto.get("selector");
        String selectorTypeStr = dto.get("selectorType");
        UIBinding.SelectorType selectorType = toSelectorType(selectorTypeStr);
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) dto.getData().get("metadata");
        if (metadata == null) metadata = Map.of();
        Map<String, Object> meta = metadata.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (Object) String.valueOf(e.getValue())));
        return new UIBinding(dto.getId(), selector != null ? selector : "", selectorType, meta);
    }

    private static UIBinding.SelectorType toSelectorType(String s) {
        if (s == null) return UIBinding.SelectorType.CSS;
        return switch (s) {
            case "CSS" -> UIBinding.SelectorType.CSS;
            case "XPATH" -> UIBinding.SelectorType.XPATH;
            case "TEXT" -> UIBinding.SelectorType.TEXT;
            case "ACTION_ID" -> UIBinding.SelectorType.ACTION_ID;
            default -> UIBinding.SelectorType.CSS;
        };
    }
}
