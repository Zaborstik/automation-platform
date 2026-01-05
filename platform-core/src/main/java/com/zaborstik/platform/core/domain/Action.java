package com.zaborstik.platform.core.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Действие - атом системы.
 * Это минимальная осмысленная операция, которую пользователь может выполнить через UI.
 * Примеры: "order_egrn_extract", "close_contract", "assign_owner"
 */
public class Action {
    private final String id;
    private final String name;
    private final String description;
    private final Set<String> applicableEntityTypes;
    private final Map<String, Object> metadata;

    public Action(String id, String name, String description, 
                  Set<String> applicableEntityTypes, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "Action id cannot be null");
        this.name = Objects.requireNonNull(name, "Action name cannot be null");
        this.description = description;
        this.applicableEntityTypes = applicableEntityTypes != null 
            ? Set.copyOf(applicableEntityTypes) 
            : Set.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getApplicableEntityTypes() {
        return applicableEntityTypes;
    }

    public boolean isApplicableTo(String entityTypeId) {
        return applicableEntityTypes.contains(entityTypeId);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return Objects.equals(id, action.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Action{id='" + id + "', name='" + name + "'}";
    }
}

