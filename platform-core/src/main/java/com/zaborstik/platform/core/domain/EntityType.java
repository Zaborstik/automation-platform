package com.zaborstik.platform.core.domain;

import java.util.Map;
import java.util.Objects;

/**
 * Тип сущности в системе.
 * Платформа не знает предметную область, поэтому EntityType - это метаданные.
 * Примеры: "Building", "Contract", "Extract"
 * 
 * Entity type in the system.
 * Platform doesn't know the domain, so EntityType is metadata.
 * Examples: "Building", "Contract", "Extract"
 */
public class EntityType {
    private final String id;
    private final String name;
    private final Map<String, Object> metadata;

    public EntityType(String id, String name, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "EntityType id cannot be null");
        this.name = Objects.requireNonNull(name, "EntityType name cannot be null");
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityType that = (EntityType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EntityType{id='" + id + "', name='" + name + "'}";
    }
}

