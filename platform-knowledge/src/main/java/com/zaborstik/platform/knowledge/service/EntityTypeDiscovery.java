package com.zaborstik.platform.knowledge.service;

import com.zaborstik.platform.core.domain.Action;
import com.zaborstik.platform.core.resolver.Resolver;
import com.zaborstik.platform.knowledge.model.PageKnowledge;
import com.zaborstik.platform.knowledge.model.UIElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EntityTypeDiscovery {

    private static final Logger log = LoggerFactory.getLogger(EntityTypeDiscovery.class);

    private static final Map<String, String> ELEMENT_TYPE_TO_ENTITY_TYPE = Map.of(
        "input", "ent-input",
        "button", "ent-button",
        "link", "ent-link",
        "form", "ent-form",
        "table", "ent-table",
        "page", "ent-page"
    );

    private final Resolver resolver;

    public EntityTypeDiscovery(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver cannot be null");
    }

    public Map<String, List<String>> discoverApplicableActions(PageKnowledge page) {
        Objects.requireNonNull(page, "page cannot be null");

        Map<String, Set<String>> intermediate = new LinkedHashMap<>();
        for (UIElement element : page.elements()) {
            String entityTypeId = ELEMENT_TYPE_TO_ENTITY_TYPE.get(element.elementType());
            if (entityTypeId == null) {
                log.warn("Unknown elementType '{}' skipped", element.elementType());
                continue;
            }

            List<Action> actions = resolver.findActionsApplicableToEntityType(entityTypeId);
            Set<String> actionIds = intermediate.computeIfAbsent(entityTypeId, ignored -> new LinkedHashSet<>());
            for (Action action : actions) {
                actionIds.add(action.id());
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : intermediate.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
}
